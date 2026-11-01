package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class PlanTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long planId;
    public LocalDate taskDate;
    public Long topicId;               // null for revision days
    public String taskType;            // READ | QUIZ | PRACTICE | REVISION
    public String title;
    public int minutes;
    public String status = "PENDING";  // PENDING | DONE | MISSED | RESCHEDULED
    public LocalDateTime completedAt;
    public int sortOrder;
}
