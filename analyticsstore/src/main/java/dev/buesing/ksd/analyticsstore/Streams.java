package dev.buesing.ksd.analyticsstore;

import dev.buesing.ksd.common.domain.PostalCodeSummary;
import dev.buesing.ksd.common.domain.PurchaseOrder;
import dev.buesing.ksd.common.domain.Store;
import dev.buesing.ksd.common.metrics.StreamsMetrics;
import dev.buesing.ksd.tools.config.CommonConfigs;
import dev.buesing.ksd.tools.serde.JsonSerde;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.ValueAndTimestamp;

@Slf4j
public class Streams {

    private static final Duration SHUTDOWN = Duration.ofSeconds(30);

    private Map<String, Object> properties(final Options options) {

        final Map<String, Object> defaults = Map.ofEntries(
                Map.entry(ProducerConfig.LINGER_MS_CONFIG, 100),
                Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()),
                Map.entry(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName()),
                Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, options.getApplicationId()),
                Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, options.getAutoOffsetReset()),
                Map.entry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true"),
                Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10000),
                Map.entry(StreamsConfig.CLIENT_ID_CONFIG, options.getClientId()),
                Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),
                Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class),
                Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG"),
                Map.entry(CommonConfigs.METRICS_REPORTER_CONFIG, options.getCustomMetricsTopic()),
                Map.entry(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 8)
                );

        final Map<String, Object> map = new HashMap<>(defaults);

        try {
            final Properties properties = new Properties();
            final File file = new File("./streams.properties");
            if (file.exists() && file.isFile()) {
                log.info("applying streams.properties");
                properties.load(new FileInputStream(file));
                map.putAll(properties.entrySet()
                        .stream()
                        .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue)));
            }
        } catch (final IOException e) {
            log.info("no streams.properties override file found");
        }

        return map;
    }


    public void start(final Options options) {

        Properties p = toProperties(properties(options));

        log.info("starting streams : " + options.getClientId());

        final Topology topology = streamsBuilder(options).build(p);

        StreamsMetrics.register(topology.describe());

        log.info("Topology:\n" + topology.describe());

        final KafkaStreams streams = new KafkaStreams(topology, p);

        streams.setUncaughtExceptionHandler(e -> {
            log.error("unhandled streams exception, shutting down.", e);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Runtime shutdown hook, state={}", streams.state());
            if (streams.state().isRunningOrRebalancing()) {
                streams.close(SHUTDOWN);
            }
        }));
    }

    private StreamsBuilder streamsBuilder(final Options options) {
        final StreamsBuilder builder = new StreamsBuilder();

        GlobalKTable<String, Store> stores = builder.globalTable(options.getStoreTopic(),
                Consumed.as("gktable-stores"),
                Materialized.as("store-global-table")
        );

        final Materialized<String, PostalCodeSummary, KeyValueStore<Bytes, byte[]>> materialized =
                Materialized.<String, PostalCodeSummary, KeyValueStore<Bytes, byte[]>>as("postal-summary")
                        .withCachingDisabled();

        builder.<String, PurchaseOrder>stream(options.getPurchaseTopic(), Consumed.as("postal-summary-source"))
                .join(stores, (k, v) -> v.getStoreId(), (purchaseOrder, store) -> {
                    purchaseOrder.setStore(store);
                    return purchaseOrder;
                }, Named.as("purchase-order-join-store"))
                .groupBy((k, v) -> v.getStore().getPostalCode(), Grouped.as("postal-summary-groupByPostalCode"))
                .aggregate(() -> null,
                        (k, incoming, aggregate) -> {

                            log.info("aggregation postalCode={}", k);

                            log.info("incoming={}", incoming);
                            log.info("aggregate={}", aggregate);

                            if (aggregate == null) {
                                aggregate = new PostalCodeSummary(incoming.getStore().getPostalCode());
                            }

                            //aggregate.getCounts().clear();

                            incoming.getItems().forEach(aggregate::addQuantity);

                            return aggregate;
                        }, Named.as("postal-summary-aggregate"), materialized)
                .toStream(Named.as("postal-summary-toStream"))
                .peek((k, v) -> log.info("XXX k={}, v={}", k, v))
                .peek((k, v) -> {
                    try {
                        log.info("Sleeping for 30 seconds ");
                        Thread.sleep(30_000);
                    } catch (final InterruptedException e) {
                        // yes, this is what I want.
                    }
                })
                .to(options.getPostalSummary(), Produced.as("postal-summary-sink"));

        // restore stream
        builder
                .<String, PostalCodeSummary>stream(options.getPostalRestore(), Consumed.as("postal-summary-restore"))
                .peek((k, v) -> log.info("restoring key={}", k), Named.as("postal-summary-restore-peek-incoming"))
                .transformValues(() -> new ValueTransformerWithKey<String, PostalCodeSummary, PostalCodeSummary>() {

                    private ProcessorContext context;
                    private KeyValueStore<String, ValueAndTimestamp<PostalCodeSummary>> store;

                    @Override
                    public void init(ProcessorContext context) {
                        this.context = context;
                        store = context.getStateStore("postal-summary");
                    }

                    @Override
                    public PostalCodeSummary transform(String key, PostalCodeSummary value) {

                        log.info("aggregation postalCode={}", key);

                        final ValueAndTimestamp<PostalCodeSummary> data = store.get(key);

                        if (data == null) {
                            store.put(key, ValueAndTimestamp.make(value, context.timestamp()));
                            log.info("created postalCode={}", value.getPostalCode());
                        } else {


                            //data.value().setMetadata(value.getMetadata());

                            store.put(key, ValueAndTimestamp.make(value, context.timestamp()));
                            //store.put(key, ValueAndTimestamp.make(data.value(), context.timestamp()));
                            log.info("updated postalCode={}", value.getPostalCode());
                        }

                        //}
                        //store.all();
                        log.info(value.toString());
                        return value;
                    }

                    @Override
                    public void close() {
                    }
                }, Named.as("postal-summary-restore-aggregate"), "postal-summary");


        return builder;
    }


    private static void dumpRecord(final ConsumerRecord<String, String> record) {
        log.info("Record:\n\ttopic     : {}\n\tpartition : {}\n\toffset    : {}\n\tkey       : {}\n\tvalue     : {}", record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

}



//        KStream<String, PostalCodeSummary> restore = builder
//                .<String, PostalCodeSummary>stream(options.getPostalRestore(), Consumed.as("postal-summary-restore"))
//                //.repartition(Repartitioned.as("FOO"))
//                .peek((k, v) -> log.info("restoring key={}", k), Named.as("postal-summary-restore-peek-incoming"));

//        restore
//                .merge(restore)
//                .transformValues(() -> new ValueTransformerWithKey<String, PostalCodeSummary, PostalCodeSummary>() {
//
//                    private ProcessorContext context;
//                    private KeyValueStore<String, ValueAndTimestamp<PostalCodeSummary>> store;
//
//                    @Override
//                    public void init(ProcessorContext context) {
//                        this.context = context;
//                        store = context.getStateStore("postal-summary");
//                    }
//
//                    @Override
//                    public PostalCodeSummary transform(String key, PostalCodeSummary value) {
//
//                        log.info("XXX");
//
//                        log.info(">>>");
//                        store.all().forEachRemaining(x -> {
//                            //KeyValue<String, ValueAndTimestamp<PostalCodeSummary>>
//                            log.info("store k={}, v={}", x.key, x.value.value());
//                        });
//                        log.info(">>>");
//
//                        if (context.headers().lastHeader("RESTORE") != null) {
//
//                            log.info("!!!aggregation postalCode={}, value={}", key, value);
//
//                            final ValueAndTimestamp<PostalCodeSummary> data = store.get(key);
//
//                            if (data == null) {
//                                store.put(key, ValueAndTimestamp.make(value, context.timestamp()));
//                                log.info("created postalCode={}", value.getPostalCode());
//                            } else {
//
//
//                                //data.value().setMetadata(value.getMetadata());
//
//                                store.put(key, ValueAndTimestamp.make(value, context.timestamp() - 1));
//                                //store.put(key, ValueAndTimestamp.make(data.value(), context.timestamp()));
//                                log.info("updated postalCode={}", value.getPostalCode());
//                            }
//
//                            log.info(value.toString());
//
//                        } else {
//                            log.info("NON RESTORE");
//                        }
//
//                        return value;
//                    }
//
//                    @Override
//                    public void close() {
//                    }
//                }, Named.as("postal-summary-restore-aggregate"), "postal-summary")
//                //.to(options.getPostalSummary(), Produced.as("postal-summary-sink"))
//                ;



//    private StreamsBuilder streamsBuilderX(final Options options) {
//
//        final StreamsBuilder builder = new StreamsBuilder();
//
//        final StoreBuilder<KeyValueStore<String, String>> storeBuilder = Stores.keyValueStoreBuilder(
//                Stores.persistentKeyValueStore("STORE_NAME"),
//                Serdes.String(),
//                Serdes.String());
//
//        builder.addStateStore(storeBuilder);
//
//        builder.<String, String>stream("in", Consumed.as("in"))
//                .transformValues(() -> new ValueTransformer<String, String>() {
//
//                            @Override
//                            public void init(ProcessorContext context) {
//
//                            }
//
//                            @Override
//                            public String transform(String value) {
//                                return null;
//                            }
//
//                            @Override
//                            public void close() {
//
//                            }
//                        },
//                        Named.as("transform1"),
//                        "STORE_NAME")
//                .selectKey((k, v) -> k, Named.as("selectKey"))
//                .to("OUT", Produced.as("to"));
//
//        builder.<String, String>stream("IN2", Consumed.as("in2"))
//                .transformValues(() -> new ValueTransformer<String, String>() {
//                            @Override
//                            public void init(ProcessorContext context) {
//
//                            }
//
//                            @Override
//                            public String transform(String value) {
//                                return null;
//                            }
//
//                            @Override
//                            public void close() {
//
//                            }
//                        },
//                        Named.as("transform2"),
//                        "STORE_NAME")
//                .selectKey((k, v) -> k, Named.as("selectKey2"))
//                .to("X", Produced.as("to2"));
//
//        return builder;
//    }

