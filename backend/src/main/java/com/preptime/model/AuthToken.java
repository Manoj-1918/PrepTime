package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class AuthToken {
    @Id public String token;
    public Long userId;
    public LocalDateTime createdAt = LocalDateTime.now();
}
