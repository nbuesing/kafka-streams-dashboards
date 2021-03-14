package dev.buesing.ksd.streams.reporter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KafkaMetricsReporter implements MetricsReporter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Duration INTERVAL = Duration.ofSeconds(5);

    final Map<String, Object> defaults = Map.ofEntries(
            Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
            Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
            Map.entry(ProducerConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO"),
            Map.entry(ProducerConfig.METRIC_REPORTER_CLASSES_CONFIG, "")

    );

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private final Map<MetricName, Pair<KafkaMetric, ObjectNode>> map = new ConcurrentHashMap<>();

    private KafkaProducer<String, String> producer;

    @Override
    public void configure(final Map<String, ?> configs) {
        final Map<String, Object> map = new HashMap<>(configs);
        map.putAll(defaults);
        producer = new KafkaProducer<>(map);
    }


    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            map.forEach((k, v) -> {
                final KafkaMetric metric = v.getKey();
                final ObjectNode node = v.getValue();

                node.put("value", v.getKey().value()); // metricValue() causing deadlocks, TBD
                node.put("timestamp", System.currentTimeMillis());

                // TODO determine a better key to use
                producer.send(
                        new ProducerRecord<>("jmx-metrics", null, null, metric.metricName().name(), serialize(node)),
                        (metadata, e) -> {
                            //TODO
                        }
                );
            });
        }
    };


    @Override
    public void init(final List<KafkaMetric> metrics) {
        metrics.forEach(metric -> {
            map.put(metric.metricName(), Pair.of(metric, jsonNode(metric)));
        });

        executor.scheduleAtFixedRate(runnable, INTERVAL.toMillis(), INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
    }

    private int i = 0;

    @Override
    public void metricChange(final KafkaMetric metric) {
        map.put(metric.metricName(), Pair.of(metric, jsonNode(metric)));
    }

    @Override
    public void metricRemoval(KafkaMetric metric) {
        map.remove(metric.metricName());
    }

    @Override
    public void close() {
        this.executor.shutdownNow();
    }

    private static String serialize(final JsonNode jsonNode) {

        if (jsonNode == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(jsonNode);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectNode jsonNode(final KafkaMetric metric) {

        final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.put("name", metric.metricName().name());
        objectNode.put("group", metric.metricName().group());

        final ObjectNode tags = JsonNodeFactory.instance.objectNode();

        metric.metricName().tags().forEach(tags::put);

        objectNode.set("tags", tags);

        return objectNode;
    }
}
