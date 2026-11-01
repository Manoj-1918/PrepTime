package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class UserDsaProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId;
    public Long problemId;
    public LocalDateTime solvedAt = LocalDateTime.now();
}
