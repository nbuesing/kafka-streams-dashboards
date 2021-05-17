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
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Streams {

    private static ExecutorService executor = Executors.newFixedThreadPool(1);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

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

        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING) {
                log.info("starting observer");
                observer.start();
            }
        });

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private StreamsBuilder streamsBuilder(final Options options) {

        final StreamsBuilder builder = new StreamsBuilder();

//        GlobalKTable<String, Store> stores = builder.globalTable(options.getStoreTopic(),
//                Consumed.as("gktable-stores"),
//                Materialized.as("store-global-table")
//        );
//
//        KTable<String, User> users = builder.table(options.getUserTopic(),
//                Consumed.as("ktable-users"),
//                Materialized.as("user-table")
//        );
//
//        KTable<String, Product> products = builder.table(options.getProductTopic(),
//                Consumed.as("ktable-products"),
//                Materialized.as("product-table")
//        );


        final Materialized<String, ProductAnalytic, WindowStore<Bytes, byte[]>> store =
                Materialized.<String, ProductAnalytic, WindowStore<Bytes, byte[]>>as("aggregate-purchase-order")
//                        .withKeySerde(Serdes.String())
//                        .withValueSerde(Serdes.Long())
                        //.withLoggingDisabled()
                        .withCachingDisabled();

//        final Materialized<String, PurchaseOrder, SessionStore<Bytes, byte[]>> store2 =
//                Materialized.<String, PurchaseOrder, SessionStore<Bytes, byte[]>>as("aggregate-purchase-order2")
//                        .withLoggingDisabled()
//                        .withCachingDisabled();

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
                            return aggregate;
                        },
                        Named.as("aggregate"),
                        store)
                .toStream(Named.as("toStream"))
                .peek((k, v) -> log.info("key={}, value={}", k, v), Named.as("peek-outgoing"));


//        final KStream<String, PurchaseOrder> orders = builder.<String, Order>stream(options.getPurchaseTopic(), Consumed.as("orders"))
//                .peek((k, v) -> log.info(k))
//                // if producer failed to key it correctly, discard it
//                .filter((k, v) -> k != null && k.equals(v.getOrderId()))
//                // map to common object (to facilitate merging)
//                .mapValues(order -> {
//                    final PurchaseOrder purchaseOrder = new PurchaseOrder();
//                    purchaseOrder.setOrder(order);
//                    return purchaseOrder;
//                });
//
//        final KStream<String, PurchaseOrder> lineItems = builder.<String, Order.LineItem>stream(options.getProductTopic(), Consumed.as("lineItems"))
//                .peek((k, v) -> log.info(k))
//                // if producer failed to key it correctly, discard it
//                .filter((k, v) -> k != null && k.equals(v.getOrderId()))
//                // map to common object (to facilitate merging)
//                .mapValues(lineItem -> {
//                    final PurchaseOrder purchaseOrder = new PurchaseOrder();
//                    purchaseOrder.getItems().add(lineItem);
//                    return purchaseOrder;
//                });
//
//        KGroupedStream<String, PurchaseOrder> gstream = orders
//                .merge(lineItems)
//                .groupByKey();
//
//        TimeWindowedKStream<String, PurchaseOrder> windowed =
//                gstream
//                        .windowedBy(
//                                SlidingWindows.withTimeDifferenceAndGrace(
//                                        Duration.ofSeconds(options.getWindowSize()),
//                                        Duration.ofSeconds(options.getGracePeriod())));
//
////        SessionWindowedKStream<String, PurchaseOrder> windowed =
////                gstream
////                        .windowedBy(
////                                SessionWindows.with(Duration.ofSeconds(options.getWindowSize())
////                        ));
//
//
//
////        TimeWindowedKStream<String, PurchaseOrder> windowed =
////                gstream
////                .windowedBy(
////                TimeWindows.of(Duration.ofSeconds(options.getWindowSize()))
////                .grace(Duration.ofSeconds(options.getGracePeriod()))
////        );
//
//
//        windowed
//                .<PurchaseOrder>aggregate(
//                        PurchaseOrder::new,
//                        (key, incoming, aggregate) -> {
//
//                            if (incoming.getOrder() != null) {
//                                aggregate.setOrder(incoming.getOrder());
//                            }
//
//                            aggregate.getItems().addAll(incoming.getItems());
//
//                            return aggregate;
//                        },
//                      //  (k, v1, v2) -> v2,
//                        Named.as("aggregate"),
//                        store
//                )
//                .toStream()
////                .transformValues(new ValueTransformerSupplier<PurchaseOrder, PurchaseOrder>() {
////                                     @Override
////                                     public ValueTransformer<PurchaseOrder, PurchaseOrder> get() {
////                                         return new ValueTransformer<>() {
////
////                                             private TimestampedWindowStore<String, PurchaseOrder> store;
////
////
////                                             @Override
////                                             @SuppressWarnings("unchecked")
////                                             public void init(ProcessorContext context) {
////                                                 store = (TimestampedWindowStore<String, PurchaseOrder>) context.getStateStore("aggregate-purchase-order");
////                                             }
////
////                                             @Override
////                                             public PurchaseOrder transform(PurchaseOrder value) {
////
////                                                 final AtomicInteger count = new AtomicInteger();
////
////                                                 store.all().forEachRemaining(i -> {
////                                                     LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(i.key.window().start()), ZoneId.systemDefault());
////                                                     LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(i.key.window().end()), ZoneId.systemDefault());
////                                                     String key = i.key.key();
////                                                     int itemCount = i.value.value().getItems().size();
////                                                     log.info("[{},{}] : {} - {}", start.toLocalTime().format(TIME_FORMATTER), end.toLocalTime().format(TIME_FORMATTER), key, itemCount);
////
////                                                     count.incrementAndGet();
////                                                     //      log.info("window key={}, value={}", i.key, i.value);
////                                                 });
////
//////                                                 log.info(">>> " + count.intValue());
////
////                                                 return value;
////                                             }
////
////                                             @Override
////                                             public void close() {
////                                             }
////                                         };
////                                     }
////                                 },
////                        Named.as("transformValue"),
////                        "aggregate-purchase-order"
////                )
//                //.peek((k, v) -> log.info("Purchase Order Updated : key={}, value={}", k, v))
//                .peek((k, v) -> log.info("Updated={}", k.key()))
//                .filter((k, v) -> v != null && v.getOrder() != null && v.getOrder().getItemCount() == v.getItems().size())
//                //.peek((k, v) -> log.info("Purchase Order Assembled : key={}, value={}", k, v))
//                .selectKey((k, v) -> v.getOrder().getOrderId())
//                .peek((k, v) -> log.info("Completed={}", k))
//                .to(options.getPurchaseOrderTopic());

        return builder;
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }
}
