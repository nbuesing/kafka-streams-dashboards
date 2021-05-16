package dev.buesing.ksd.analytics;

import dev.buesing.ksd.common.domain.PurchaseOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class StateObserver {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private ReadOnlyWindowStore<String, Long> store;
    //private ReadOnlySessionStore<String, PurchaseOrder> store;


    private KafkaStreams streams;

    private boolean isRunning = false;

    public StateObserver(final KafkaStreams streams) {
        this.streams = streams;
    }

    public void start() {

        if (isRunning) {
            return;
        }

        isRunning = true;
        this.store = streams.store(StoreQueryParameters.fromNameAndType("aggregate-purchase-order", QueryableStoreTypes.windowStore()));
 //       this.store = streams.store(StoreQueryParameters.fromNameAndType("pickup-order-reduce-store", QueryableStoreTypes.sessionStore()));

        System.out.println(store.getClass());

        Thread t = new Thread(() -> {


            while (true) {

                log.info("\n\nStatestore");

                log.info("XXXX");


                store.all().forEachRemaining(i -> {

                    LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(i.key.window().start()), ZoneId.systemDefault());
                    LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(i.key.window().end()), ZoneId.systemDefault());
                    String key = i.key.key();
               //     int itemCount = i.value.getItems().size();
                    log.info("[{},{}] : {} - {}", start.toLocalTime().format(TIME_FORMATTER), end.toLocalTime().format(TIME_FORMATTER), key, i.value);
                });
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
        });
        t.start();

    }

}
