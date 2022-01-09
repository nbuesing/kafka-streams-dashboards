package dev.buesing.ksd.analytics.domain;

import dev.buesing.ksd.common.domain.ProductAnalytic;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;


public class BySku implements By {

    private TreeMap<String, TreeMap<Window, ProductAnalyticSummary>> records;

    public BySku() {
        this.records = new TreeMap<>();
    }


    public void add(final org.apache.kafka.streams.kstream.Window kstreamWindow, final ProductAnalytic productAnalytic) {
        final Window window = Window.convert(kstreamWindow);

        final String sku = productAnalytic.getSku();

        if (!records.containsKey(sku)) {
            records.put(productAnalytic.getSku(), new TreeMap<>());
        }

        productAnalytic.setSku(window.toString());

        records.get(sku).put(window, ProductAnalyticSummary.create(window, productAnalytic));
    }

    public TreeMap<String, TreeMap<Window, ProductAnalyticSummary>> getRecords() {
        return records;
    }

}
