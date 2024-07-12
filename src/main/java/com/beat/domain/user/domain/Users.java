package com.beat.domain.user.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder
    public Users() {
    }

    public static Users create() {
        return Users.builder()
                .build();
    }
}