package com.ecommerce.orderservice.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address implements Serializable {

    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phone;
    private String fullName;
}
