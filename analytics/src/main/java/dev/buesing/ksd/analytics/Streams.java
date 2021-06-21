package dev.buesing.ksd.analytics;

import dev.buesing.ksd.common.domain.ProductAnalytic;
import dev.buesing.ksd.common.domain.PurchaseOrder;
import dev.buesing.ksd.common.metrics.StreamsMetrics;
import dev.buesing.ksd.tools.serde.JsonSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.SessionStore;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class Streams {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER_SSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Map<String, Object> properties(final Options options) {
        return Map.ofEntries(
                Map.entry(ProducerConfig.LINGER_MS_CONFIG, 100),
                Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()),
                Map.entry(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName()),
                Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, options.getApplicationId()),
                Map.entry(StreamsConfig.CLIENT_ID_CONFIG, options.getClientId()),
                Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, options.getAutoOffsetReset()),
                Map.entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"),
                Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100),
                Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),
                Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "TRACE"),
                Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName())
        );
    }

    private final Options options;

    public Streams(Options options) {
        this.options = options;
    }


    public void start() {

        Properties p = toProperties(properties(options));

        log.info("starting streams " + options);

        final Topology topology = streamsBuilder().build(p);

        StreamsMetrics.register(topology.describe());

        log.info("Topology:\n" + topology.describe());

        final KafkaStreams streams = new KafkaStreams(topology, p);

        streams.setUncaughtExceptionHandler(e -> {
            log.error("unhandled streams exception, shutting down.", e);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        final StateObserver observer = new StateObserver(streams);
        final ServletDeployment servletDeployment = new ServletDeployment(observer);

        servletDeployment.start();
    }

    private StreamsBuilder streamsBuilder() {
        switch (options.getWindowType()) {
            case TUMBLING:
                return streamsBuilderTumbling();
            case HOPPING:
                return streamsBuilderHopping();
            case SLIDING:
                return streamsBuilderSliding();
            case SESSION:
                return streamsBuilderSession();
            default:
                return null;
        }
    }

    private StreamsBuilder streamsBuilderTumbling() {

        final StreamsBuilder builder = new StreamsBuilder();

        final Materialized<String, ProductAnalytic, WindowStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, WindowStore<Bytes, byte[]>>as("TUMBLING-aggregate-purchase-order")
                //.withLoggingDisabled()
                .withCachingDisabled()
                ;

        builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("TUMBLING-line-item"))
                .peek((k, v) -> log.info("key={}", k), Named.as("TUMBLING-peek-incoming"))
                .groupByKey(Grouped.as("TUMBLING-groupByKey"))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(options.getWindowSize()))
                        .grace(Duration.ofSeconds(options.getGracePeriod())))
                .aggregate(Streams::initialize,
                        Streams::aggregator,
                        Named.as("TUMBLING-aggregate"),
                        store)
                .toStream(Named.as("TUMBLING-toStream"))
                .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("TUMBLING-peek-outgoing"))
                .selectKey((k, v) -> k.key() + " [" + convert(k.window().startTime()) + "," + convert(k.window().endTime()) + ")")
                .mapValues(Streams::minimize)
                .to(options.getOutputTopic(), Produced.as("TUMBLING-to"));


        return builder;
    }

    private StreamsBuilder streamsBuilderHopping() {

        final StreamsBuilder builder = new StreamsBuilder();

        final Materialized<String, ProductAnalytic, WindowStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, WindowStore<Bytes, byte[]>>as("HOPPING-aggregate-purchase-order")
                //.withLoggingDisabled()
                .withCachingDisabled()
                ;

        builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("HOPPING-line-item"))
                .peek((k, v) -> log.info("key={}", k), Named.as("HOPPING-peek-incoming"))
                .groupByKey(Grouped.as("HOPPING-groupByKey"))
                .windowedBy(TimeWindows.of(Duration.ofSeconds(options.getWindowSize()))
                        .advanceBy(Duration.ofSeconds(options.getWindowSize() / 2))
                        .grace(Duration.ofSeconds(options.getGracePeriod())))
                .aggregate(Streams::initialize,
                        Streams::aggregator,
                        Named.as("HOPPING-aggregate"),
                        store)
                .toStream(Named.as("HOPPING-toStream"))
                .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("HOPPING-peek-outgoing"))
                .selectKey((k, v) -> k.key() + " [" + convert(k.window().startTime()) + "," + convert(k.window().endTime()) + ")")
                .mapValues(Streams::minimize)
                .to(options.getOutputTopic(), Produced.as("HOPPING-to"));


        return builder;
    }

    private StreamsBuilder streamsBuilderSliding() {

        final StreamsBuilder builder = new StreamsBuilder();

        final Materialized<String, ProductAnalytic, WindowStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, WindowStore<Bytes, byte[]>>as("SLIDING-aggregate-purchase-order")
                //.withLoggingDisabled()
                .withCachingDisabled()
                ;

        builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("SLIDING-line-item"))
                .peek((k, v) -> log.info("key={}", k), Named.as("SLIDING-peek-incoming"))
                .groupByKey(Grouped.as("SLIDING-groupByKey"))
                .windowedBy(SlidingWindows.withTimeDifferenceAndGrace(
                        Duration.ofSeconds(options.getWindowSize()),
                        Duration.ofSeconds(options.getGracePeriod())))
                .aggregate(Streams::initialize,
                        Streams::aggregator,
                        Named.as("SLIDING-aggregate"),
                        store)
                .toStream(Named.as("SLIDING-toStream"))
                .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("SLIDING-peek-outgoing"))
                .selectKey((k, v) -> k.key() + " [" + convert(k.window().startTime()) + "," + convert(k.window().endTime()) + ")")
                .mapValues(Streams::minimize)
                .to(options.getOutputTopic(), Produced.as("SLIDING-to"));

        return builder;
    }

    private StreamsBuilder streamsBuilderSession() {

        final StreamsBuilder builder = new StreamsBuilder();

        final Materialized<String, ProductAnalytic, SessionStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, SessionStore<Bytes, byte[]>>as("SESSION-aggregate-purchase-order")
                //.withLoggingDisabled()
                //.withCachingDisabled()
                ;

        builder.<String, PurchaseOrder>stream(options.getTopic(), Consumed.as("SESSION-line-item"))
                .peek((k, v) -> log.info("key={}", k), Named.as("SESSION-peek-incoming"))
                .groupByKey(Grouped.as("SESSION-groupByKey"))
                .windowedBy(SessionWindows.with(Duration.ofSeconds(options.getWindowSize())))
                .aggregate(Streams::initialize,
                        Streams::aggregator,
                        Streams::mergeSessions,
                        Named.as("SESSION-aggregate"),
                        store)
                .toStream(Named.as("SESSION-toStream"))
                .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("SESSION-peek-outgoing"))
                .selectKey((k, v) -> k.key() + " [" + convert(k.window().startTime()) + "," + convert(k.window().endTime()) + ")")
                .mapValues(Streams::minimize)
                .to(options.getOutputTopic(), Produced.as("SESSION-to"));

        return builder;
    }

    private static ProductAnalytic initialize() {
        return new ProductAnalytic();
    }

    private static ProductAnalytic aggregator(final String key, final PurchaseOrder value, final ProductAnalytic aggregate) {
        aggregate.setSku(key);
        aggregate.setQuantity(aggregate.getQuantity() + quantity(value, key));
        aggregate.addOrderId(value.getOrderId());
        aggregate.setTimestamp(value.getTimestamp());
        return aggregate;
    }

    // merge into left thinking this would keep the list of orderIds the same - but merge could go either way.
    private static ProductAnalytic mergeSessions(final String key, final ProductAnalytic left, final ProductAnalytic right) {

        log.debug("merging session windows for key={}", key);

        left.setQuantity(left.getQuantity() + right.getQuantity());
        left.getOrderIds().addAll(right.getOrderIds());

        if (left.getTimestamp() == null || right.getTimestamp().isAfter(left.getTimestamp())) {
            left.setTimestamp(right.getTimestamp());
        }

        return left;
    }

    private static long quantity(final PurchaseOrder value, final String sku) {
        return value.getItems().stream().filter(i -> i.getSku().equals(sku)).findFirst().map(i -> (long) i.getQuantity()).orElse(0L);
    }

    private static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

    private static String convert(final Instant ts) {
        if (ts == null) {
            return null;
        }
        return LocalDateTime.ofInstant(ts, ZoneId.systemDefault()).format(TIME_FORMATTER_SSS);
    }


    private static Map<String, Object> minimize(final ProductAnalytic productAnalytic) {

        if (productAnalytic == null) {
            return null;
        }

        return Map.ofEntries(
                Map.entry("qty", productAnalytic.getQuantity()),
                Map.entry("ts", convert(productAnalytic.getTimestamp()))
        );
    }
}
