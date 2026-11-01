package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    @Column(unique = true, nullable = false) public String email;
    @JsonIgnore public String passwordHash;
    public LocalDateTime createdAt = LocalDateTime.now();
}
