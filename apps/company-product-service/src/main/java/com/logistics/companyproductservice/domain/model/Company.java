package com.logistics.companyproductservice.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_companies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    private Company(String name, Type type, UUID hubId, String address) {
        this.name = name;
        this.type = type;
        this.hubId = hubId;
        this.address = address;
    }

    public static Company create(String name, Type type, UUID hubId, String address) {
        return new Company(name, type, hubId, address);
    }

    public void update(String name, String address) {
        if (name != null) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
    }

    public enum Type {
        PRODUCER,
        RECEIVER
    }
}