package dev.buesing.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class ProductAnalytic {

    private String sku;
    private Long quantity = 0L;
    private List<String> orderIds = new ArrayList<>();

    public void addOrderId(final String orderId) {
        orderIds.add(orderId);
    }

}
