package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class AttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long attemptId;
    public Long userId;
    public Long topicId;
    public Long questionId;
    public int selectedIndex;
    public boolean correct;
}
