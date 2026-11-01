package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class Subject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name;
    public String category;      // APTITUDE | DSA | CORE | SYSTEM_DESIGN
    @Column(columnDefinition = "text") public String description;
    public int weight;           // share of study time used by the scheduler
    public int sortOrder;
}
