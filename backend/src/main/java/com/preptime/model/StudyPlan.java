package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class StudyPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId;
    public LocalDate startDate;
    public int totalDays;
    public double hoursPerDay;
    public String status = "ACTIVE";   // ACTIVE | ARCHIVED
    public int version = 1;            // bumped on every reschedule
    @Column(columnDefinition = "text") public String aiSummary;
    public LocalDateTime createdAt = LocalDateTime.now();
    @Transient public int overflow;    // tasks that did not fit (only set when the plan is created)
}
