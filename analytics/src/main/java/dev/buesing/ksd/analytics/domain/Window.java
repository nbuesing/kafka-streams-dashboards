package dev.buesing.ksd.analytics.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = {"start", "end"})
public class Window implements Comparable<Window> {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final long start;
    private final long end;

    @Override
    public int compareTo(final Window o) {
        if (this == o) {
            return 0;
        } else if (start < o.getStart()) {
            return -1;
        } else if (start > o.getStart()) {
            return 1;
        } else if (end < o.getEnd()) {
            return -1;
        } else if (end > o.getEnd()) {
            return 1;
        } else {
            return 0;
        }
    }

    public String toString() {
        return "[" + convert(start) + ", " + convert(end) + "]";
    }

    public String start() {
        return convert(start);
    }

    public String end() {
        return convert(end);
    }

    private String convert(final long ts) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(TIME_FORMATTER);
    }

    /**
     * need a comparable window.
     */
    public static Window convert(final org.apache.kafka.streams.kstream.Window window) {
        return new Window(window.start(), window.end());
    }

}
