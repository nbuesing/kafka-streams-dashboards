/*
 * Copyright (c) 2020. 
 *
 * This code is provided as-is w/out warranty. 
 *  
 */

package dev.buesing.ksd.common.config;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseOptions {

    @Parameter(names = "--help", help = true, hidden = true)
    private boolean help;

    @Parameter(names = { "-b", "--bootstrap-servers" }, description = "cluster bootstrap servers")
    private String bootstrapServers = "localhost:19092,localhost:29092,localhost:39092";

    @Parameter(names = { "--store-topic" }, description = "compacted topic holding stores")
    private String storeTopic = "orders-store";

    @Parameter(names = { "--user-topic" }, description = "compacted topic holding users")
    private String userTopic = "orders-user";

    @Parameter(names = { "--product-topic" }, description = "compacted topic holding products")
    private String productTopic = "orders-product";

    @Parameter(names = { "--purchase-topic" }, description = "")
    private String purchaseTopic = "orders-purchase";

    @Parameter(names = { "--pickup-topic" }, description = "")
    private String pickupTopic = "orders-pickup";

    @Parameter(names = { "--custom-metrics-topic" }, description = "custom metrics topic")
    private String customMetricsTopic = "_metrics-kafka-streams";


    private int numberOfStores = 1000;
    private int numberOfUsers = 10_000;
    private int numberOfProducts = 10_000;
    private int maxQuantity = 10;

}