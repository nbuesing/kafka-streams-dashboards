package dev.buesing.ksd.analytics.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dev.buesing.ksd.analytics.domain.ByWindow;
import dev.buesing.ksd.analytics.domain.Window;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class WindowSerializer extends StdSerializer<Window> {

    public WindowSerializer() {
        this(null);
    }

    public WindowSerializer(final Class<Window> vc) {
        super(vc);
    }

    public void serialize(final Window value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }
}
