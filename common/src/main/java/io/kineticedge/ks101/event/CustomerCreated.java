package io.kineticedge.ks101.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.kineticedge.ks101.domain.Email;
import io.kineticedge.ks101.domain.Phone;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class CustomerCreated {

    private String customerId;
    private String lastName;
    private String firstName;
    private Email email;
    private Phone phone;
}
