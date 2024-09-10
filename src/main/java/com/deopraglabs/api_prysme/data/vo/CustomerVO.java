package com.deopraglabs.api_prysme.data.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder("id")
public class CustomerVO extends RepresentationModel<CustomerVO> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private long key;
    private String cpfCnpj;
    private String name;
    private String tradeName;
    private String email;
    private LocalDate birthFoundationDate;
    private String stateRegistration;
    private List<String> phoneNumbers = new ArrayList<>();
    private AddressVO address;
    private CartVO cart;

}
