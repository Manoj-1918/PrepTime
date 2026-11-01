package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId;
    public Long topicId;               // null = mock test
    public int score;
    public int total;
    public LocalDateTime takenAt = LocalDateTime.now();
}
