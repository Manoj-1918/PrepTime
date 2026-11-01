package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long subjectId;
    public String name;
    public double estHours;
    public int sortOrder;
}
