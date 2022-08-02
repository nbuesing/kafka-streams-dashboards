package dev.buesing.ksd.restore;

import dev.buesing.ksd.common.domain.PostalCodeSummary;
import dev.buesing.ksd.common.domain.ProductAnalytic;
import dev.buesing.ksd.common.domain.PurchaseOrder;
import dev.buesing.ksd.tools.serde.JsonDeserializer;
import dev.buesing.ksd.tools.serde.JsonSerializer;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;

@Slf4j
public class Restore {

    private final Options options;

    public Restore(final Options options) {
        this.options = options;
    }

    public void start() {

        final KafkaProducer<String, PostalCodeSummary> producer = new KafkaProducer<>(producer(options));

        int count = 0;

        while (true) {


            PostalCodeSummary s = new PostalCodeSummary("55053");
            s.setMetadata("cleared");

            Headers headers = new RecordHeaders();
            headers.add("RESTORE", "true".getBytes());

            log.info("Sending key={}, value={}", s.getPostalCode(), s);
            s.setMetadata("" + (count++));
            producer.send(new ProducerRecord<>(options.getPostalRestore(), null, s.getPostalCode(), s, headers), (metadata, exception) -> {
                if (exception != null) {
                    log.error("error producing to kafka", exception);
                } else {
                    log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });


            producer.flush();

            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public void start2() {

        final KafkaConsumer<String, PostalCodeSummary> consumer = new KafkaConsumer<>(consumer(options));
        final KafkaProducer<String, PostalCodeSummary> producer = new KafkaProducer<>(producer(options));

        consumer.subscribe(Collections.singleton(options.getChangelogTopic()));

        while (true) {
            ConsumerRecords<String, PostalCodeSummary> records = consumer.poll(Duration.ofMillis(500L));

            records.forEach(record -> {

                PostalCodeSummary s = new PostalCodeSummary("55051");
                s.setMetadata("cleared");

                record.headers().add("RESTORE", "true".getBytes());
                //TODO HEADER!!!!

                int total = record.value().getCounts().entrySet().stream().map(e -> e.getValue()).reduce(0, Integer::sum);
                record.value().setMetadata("totalQuantity=" + total);

                log.info("Sending key={}, value={}", record.key(), record.value());
                producer.send(new ProducerRecord<>(options.getPostalRestore(), null, record.key(), record.value(), record.headers()), (metadata, exception) -> {
                    if (exception != null) {
                        log.error("error producing to kafka", exception);
                    } else {
                        log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });
            });

            producer.flush();
        }

        //producer.close();
        //consumer.close();
    }

    private Map<String, Object> consumer(final Options options) {
        return Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()),
                Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName()),
                Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                Map.entry(ConsumerConfig.GROUP_ID_CONFIG, "ABC001")
        );
    }

    private Map<String, Object> producer(final Options options) {
        return Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
                Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName()),
                Map.entry(ProducerConfig.ACKS_CONFIG, "all")
        );
    }

//    private static void dumpRecord(final ConsumerRecord<String, String> record) {
//        log.info("Record:\n\ttopic     : {}\n\tpartition : {}\n\toffset    : {}\n\tkey       : {}\n\tvalue     : {}", record.topic(), record.partition(), record.offset(), record.key(), record.value());
//    }

}
