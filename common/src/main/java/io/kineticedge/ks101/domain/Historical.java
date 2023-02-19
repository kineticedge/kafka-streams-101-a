package io.kineticedge.ks101.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.Instant;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class Historical<T> implements Comparable<Historical<T>> {

    private Instant start;
    private Instant end;
    private T element;

    @Override
    public int compareTo(final Historical<T> other) {
        return this.start.compareTo(other.start);
    }

}
