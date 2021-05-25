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
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class Streams {

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

    public void start(final Options options) {

        Properties p = toProperties(properties(options));

        log.info("starting streams " + options);

        final Topology topology = streamsBuilder(options).build(p);

        StreamsMetrics.register(topology.describe());

        log.info("Topology:\n" + topology.describe());

        final KafkaStreams streams = new KafkaStreams(topology, p);

        streams.setUncaughtExceptionHandler(e -> {
            log.error("unhandled streams exception, shutting down.", e);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });

        final StateObserver observer = new StateObserver(streams);


//        streams.setStateListener((newState, oldState) -> {
//            if (newState == KafkaStreams.State.RUNNING) {
//                log.info("starting observer");
//                observer.start();
//            }
//        });

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        final ServletDeployment servletDeployment = new ServletDeployment(observer);
        servletDeployment.start();
    }

    private StreamsBuilder streamsBuilder(final Options options) {

        final StreamsBuilder builder = new StreamsBuilder();

        final Materialized<String, ProductAnalytic, WindowStore<Bytes, byte[]>> store = Materialized.<String, ProductAnalytic, WindowStore<Bytes, byte[]>>as("aggregate-purchase-order")
                        //.withLoggingDisabled()
                        .withCachingDisabled();

        builder.<String, PurchaseOrder>stream("pickup-order-handler-purchase-order-join-product-repartition", Consumed.as("line-item"))
                .peek((k, v) -> log.debug("key={}", k), Named.as("peek-incoming"))
                .groupByKey(Grouped.as("groupByKey"))
                .windowedBy(SlidingWindows.withTimeDifferenceAndGrace(
                        Duration.ofSeconds(options.getWindowSize()),
                        Duration.ofSeconds(options.getGracePeriod())))
                .aggregate(ProductAnalytic::new,
                        (key, value, aggregate) -> {
                            if (aggregate.getSku() == null) {
                                aggregate.setSku(key);
                            }
                            PurchaseOrder.LineItem item = value.getItems().stream().filter(i -> i.getSku().equals(key)).findFirst().get();
                            aggregate.setQuantity(aggregate.getQuantity() + (long) item.getQuantity());
                            aggregate.addOrderId(value.getOrderId());
                            aggregate.setOrderTimestamp(value.getTimestamp());
                            return aggregate;
                        },
                        Named.as("aggregate"),
                        store)
                .toStream(Named.as("toStream"))
                .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("peek-outgoing"));

        return builder;
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }
}
