package io.kineticedge.ks101.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "$type")
public class Customer360 {

    private String customerId;
    private Name name;

    private List<Historical<Name>> names;
    private List<Historical<Email>> emails;
    private List<Historical<Phone>> phones;
}
