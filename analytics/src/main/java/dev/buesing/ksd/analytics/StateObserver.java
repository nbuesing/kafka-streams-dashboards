package dev.buesing.ksd.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.buesing.ksd.common.domain.ProductAnalytic;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class StateObserver {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @AllArgsConstructor
    public static class WindowedKey implements Comparable<WindowedKey> {
        private String key;
        private Instant start;
        private Instant end;

        @Override
        public int compareTo(WindowedKey o) {
            int compareTo = key.compareTo(o.key);
            if (compareTo == 0) {
                compareTo = start.compareTo(o.start);
            }
            if (compareTo == 0) {
                compareTo = end.compareTo(o.end);
            }
            return compareTo;
        }

        public String toString() {
            return key + " [" + convert(start) + "," + convert(end) + "]";
        }

        public String start() {
            return convert(start);
        }

        public String end() {
            return convert(end);
        }

        private String convert(final Instant ts) {
            return LocalDateTime.ofInstant(ts, ZoneId.systemDefault()).format(TIME_FORMATTER);
        }

        public static WindowedKey create(Windowed<String> window) {
            return new WindowedKey(window.key(), window.window().startTime(), window.window().endTime());
        }
    }

    private ReadOnlyWindowStore<String, ProductAnalytic> store;
    //private ReadOnlySessionStore<String, PurchaseOrder> store;


    private KafkaStreams streams;


    public StateObserver(final KafkaStreams streams) {
        this.streams = streams;
    }

    public JsonNode getState() {

        this.store = streams.store(StoreQueryParameters.fromNameAndType("aggregate-purchase-order", QueryableStoreTypes.windowStore()));
        //        this.store = streams.store(StoreQueryParameters.fromNameAndType("pickup-order-reduce-store", QueryableStoreTypes.sessionStore()));

        final Map<WindowedKey, ProductAnalytic> map = new TreeMap<>();

        store.all().forEachRemaining(i -> {
            map.put(WindowedKey.create(i.key), i.value);
        });

        final ArrayNode elements = JsonNodeFactory.instance.arrayNode();

        map.forEach((k, v) -> {
            //log.info("{} - {}", k, v);
            ObjectNode element = JsonNodeFactory.instance.objectNode();
            ObjectNode key = JsonNodeFactory.instance.objectNode();
            key.put("sku", k.key);
            key.put("start", k.start());
            key.put("end", k.end());
            ObjectNode value = JsonNodeFactory.instance.objectNode();
            value.put("quantity", v.getQuantity());
            ArrayNode orderIds = JsonNodeFactory.instance.arrayNode();
            v.getOrderIds().forEach(orderId -> orderIds.add(orderId));
            value.set("order-ids", orderIds);
            value.put("timestamp", v.orderTimestamp());
            element.set("key", key);
            element.set("value", value);

            elements.add(element);
        });

        return elements;
    }


    //                store.fetch(Producer.prefix + "_001").forEachRemaining(i -> {
//                    LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(i.key.window().start()), ZoneId.systemDefault());
//                    LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(i.key.window().end()), ZoneId.systemDefault());
//                    String key = i.key.key();
//                    int itemCount = i.value.getItems().size();
//                    log.info(">>> [{},{}] : {} - {}", start.toLocalTime().format(TIME_FORMATTER), end.toLocalTime().format(TIME_FORMATTER), key, itemCount);
//                });
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }


}
