package io.kineticedge.ks101.consumer.serde;


import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.TimeZone;

import io.kineticedge.ks101.common.InstantDeserializer;
import io.kineticedge.ks101.common.InstantSerializer;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class JsonSerializer<T> implements Serializer<T> {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setTimeZone(TimeZone.getDefault())
                    .registerModule(new JavaTimeModule())
                    .registerModule(new SimpleModule("instant-module", new Version(1, 0, 0, null, "", ""))
                            .addSerializer(Instant.class, new InstantSerializer())
                            .addDeserializer(Instant.class, new InstantDeserializer())
                    )
                    ;

    // needed by kafka
    @SuppressWarnings("unused")
    public JsonSerializer() {
    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {

        if (data == null)
            return null;

        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }

    @Override
    public void close() {
    }
}
