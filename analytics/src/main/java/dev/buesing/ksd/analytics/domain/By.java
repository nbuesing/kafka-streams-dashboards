package dev.buesing.ksd.analytics.domain;

import dev.buesing.ksd.common.domain.ProductAnalytic;

public interface By {

    void add(org.apache.kafka.streams.kstream.Window window, ProductAnalytic productAnalytic);
}
