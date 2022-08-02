package dev.buesing.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class PostalCodeSummary {

    private String postalCode;

    private String metadata;

    public PostalCodeSummary(final String postalCode) {
        this.postalCode = postalCode;
    }

    private Map<String, Integer> counts = new HashMap<>();

    public void addQuantity(PurchaseOrder.LineItem lineItem) {
        counts.merge(lineItem.getSku(), lineItem.getQuantity(), Integer::sum);
    }
}
