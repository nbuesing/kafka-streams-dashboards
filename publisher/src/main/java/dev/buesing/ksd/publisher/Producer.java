package dev.buesing.ksd.publisher;

import dev.buesing.ksd.common.domain.PurchaseOrder;
import dev.buesing.ksd.tools.serde.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class Producer {

    private static final Random RANDOM = new Random();

    private static final String ORDER_PREFIX = RandomStringUtils.randomAlphabetic(2).toUpperCase(Locale.ROOT);

    private final Options options;

    public Producer(final Options options) {
        this.options = options;
    }

    private String getRandomSku(int index) {
        System.out.println("1>>>>>>");

        if (options.getSkus() == null) {
            System.out.println("2>>>>>>");
            return StringUtils.leftPad(Integer.toString(RANDOM.nextInt(options.getNumberOfProducts())), 10, '0');
        } else {
            System.out.println("3>>>>>>");

            final int productId = options.getSkus().get(index);

            if (productId < 0 || productId >= options.getNumberOfProducts()) {
                System.out.println("4>>>>>>");
                throw new IllegalArgumentException("invalid product number");
            }

            return StringUtils.leftPad(Integer.toString(productId), 10, '0');
        }
    }

    private String getRandomUser() {
        return Integer.toString(RANDOM.nextInt(options.getNumberOfUsers()));
    }

    private String getRandomStore() {
        return Integer.toString(RANDOM.nextInt(options.getNumberOfStores()));
    }

    private int getRandomItemCount() {

        if (options.getLineItemCount().indexOf('-') < 0) {
            return Integer.parseInt(options.getLineItemCount());
        } else {
            String[] split = options.getLineItemCount().split("-");
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            return RANDOM.nextInt(max + 1 - min) + min;
        }
    }

    private int getRandomQuantity() {
        return RANDOM.nextInt(options.getMaxQuantity()) + 1;
    }

    private static int counter = 0;

    private static String orderNumber() {
        return ORDER_PREFIX + "-" + (counter++);
    }

    private PurchaseOrder createPurchaseOrder() {
        PurchaseOrder purchaseOrder = new PurchaseOrder();

        purchaseOrder.setTimestamp(Instant.now());
        purchaseOrder.setOrderId(orderNumber());
        purchaseOrder.setUserId(getRandomUser());
        purchaseOrder.setStoreId(getRandomStore());
        purchaseOrder.setItems(IntStream.range(0, getRandomItemCount())
                .boxed()
                .map(i -> {
                    final PurchaseOrder.LineItem item = new PurchaseOrder.LineItem();
                    item.setSku(getRandomSku(i));
                    item.setQuantity(getRandomQuantity());
                    item.setQuotedPrice(null); // TODO remove from domain
                    return item;
                })
                .collect(Collectors.toList())
        );

        return purchaseOrder;
    }

    public void start() {

//        if (options.getSkus() != null && options.getSkus().size() != options.getLineItemCount()) {
//            System.out.println("XXXX");
//            throw new IllegalArgumentException("!=");
//        }

        System.out.println(">>>");

        final KafkaProducer<String, PurchaseOrder> kafkaProducer = new KafkaProducer<>(properties(options));

        while (true) {

            PurchaseOrder purchaseOrder = createPurchaseOrder();

            log.info("Sending key={}, value={}", purchaseOrder.getOrderId(), purchaseOrder);
            kafkaProducer.send(new ProducerRecord<>(options.getPurchaseTopic(), null, purchaseOrder.getOrderId(), purchaseOrder), (metadata, exception) -> {
                if (exception != null) {
                    log.error("error producing to kafka", exception);
                } else {
                    log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

            try {
                Thread.sleep(options.getPause());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // kafkaProducer.close();

    }

    private Map<String, Object> properties(final Options options) {
        return Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
                Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName()),
                Map.entry(ProducerConfig.ACKS_CONFIG, "all")
        );
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
