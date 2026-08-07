package com.logistics.companyproductservice.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_companies")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    private UUID id;


    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Type type;

    @Column(nullable = false)
    private UUID hubId;


    @Column(nullable = false, length = 255)
    private String address;

    public static Company create(String name, Type type, UUID hubId, String address) {
        UUID id = UUID.randomUUID();
        return new Company(id, name, type, hubId, address);
    }

    public enum Type {
        PRODUCER,
        RECEIVER
    }
}