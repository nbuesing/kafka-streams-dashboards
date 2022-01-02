package dev.buesing.ksd.common.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class ProductAnalyticSummary {

    private static final DateTimeFormatter TIME_FORMATTER_SSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private String sku;
    private Long qty;
    private String ts;

    public static ProductAnalyticSummary create(final ProductAnalytic productAnalytic) {
        return new ProductAnalyticSummary(productAnalytic.getSku(), productAnalytic.getQuantity(), convert(productAnalytic.getTimestamp()));
    }


    private static String convert(final Instant ts) {
        if (ts == null) {
            return null;
        }
        return LocalDateTime.ofInstant(ts, ZoneId.systemDefault()).format(TIME_FORMATTER_SSS);
    }

}
