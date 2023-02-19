package io.kineticedge.ks101.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.kineticedge.ks101.domain.Email;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class EmailUpdated {
    private String customerId;
    private Email email;
}