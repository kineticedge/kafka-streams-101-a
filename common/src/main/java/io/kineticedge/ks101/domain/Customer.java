package io.kineticedge.ks101.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class Customer {

    private String customerId;
    private String lastName;
    private String firstName;
    private Email email;
    private Phone phone;
}
