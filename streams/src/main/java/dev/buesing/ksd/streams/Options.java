/*
 * Copyright (c) 2020. 
 *
 * This code is provided as-is w/out warranty. 
 *  
 */

package dev.buesing.ksd.streams;

import com.beust.jcommander.Parameter;
import dev.buesing.ksd.common.config.BaseOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
@Setter
public class Options extends BaseOptions  {

    @Parameter(names = { "-g", "--application-id" }, description = "application id")
    private String applicationId = "pickup-order-handler";

    @Parameter(names = { "--client-id" }, description = "client id")
    private String clientId = "s-" + UUID.randomUUID();

    @Parameter(names = { "--auto-offset-reset" }, description = "where to start consuming from if no offset is provided")
    private String autoOffsetReset = "earliest";

}
