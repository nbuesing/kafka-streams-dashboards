package dev.buesing.ksd.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.buesing.ksd.analytics.domain.*;
import dev.buesing.ksd.common.domain.ProductAnalytic;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class StateObserver {


    private final KafkaStreams streams;

    private final Options.WindowType windowType;

    private final String storeName;

    public StateObserver(final KafkaStreams streams, final Options.WindowType windowType) {
        this.streams = streams;
        this.windowType = windowType;
        this.storeName = windowType.name() + "-aggregate-purchase-order";
    }

    public By getState(String type) {

        log.info("query: windowType={}, type={}", windowType, type);

        if ("windowing".equals(type)) {
            if (windowType == Options.WindowType.SESSION) {
                return populateSession(new ByWindow());
            } else if (windowType == Options.WindowType.NONE) {
                return populateKeyValue(new ByFoo());
            } else {
                return populateWindow(new ByWindow());
            }
        } else {
            if (windowType == Options.WindowType.SESSION) {
                return populateSession(new BySku());
            } else if (windowType == Options.WindowType.NONE) {
                return populateKeyValue(new ByFoo());
            } else {
                return populateWindow(new BySku());
            }
        }
    }

    private By populateWindow(By by) {
        ReadOnlyWindowStore<String, ProductAnalytic> store = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.windowStore()));
        store.all().forEachRemaining(i -> {
            by.add(i.key.window(), i.value);
        });
        return by;
    }

    private By populateSession(By by) {
        ReadOnlySessionStore<String, ProductAnalytic> session = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.sessionStore()));
        session.fetch("0000000000", "9999999999").forEachRemaining(i -> {
            by.add(i.key.window(), i.value);
        });
        return by;
    }

    private By populateKeyValue(By by) {
        ReadOnlyKeyValueStore<String, ValueAndTimestamp<ProductAnalytic>> store = streams.store(StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.timestampedKeyValueStore()));
        store.all().forEachRemaining(i -> {
            by.add(null, i.value.value());
        });
        return by;
    }



}
