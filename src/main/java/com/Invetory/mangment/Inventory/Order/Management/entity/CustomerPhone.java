package com.Invetory.mangment.Inventory.Order.Management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "customers_phone")
public class CustomerPhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "Cus_id", nullable = false)
    private Customer customer;

    @NotBlank
    @Column(name = "phone_Number")
    private String phoneNumber;
}
