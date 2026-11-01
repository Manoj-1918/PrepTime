package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class DsaProblem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long topicId;
    public String title;
    public String url;
    public String difficulty;
    public String sheet;   // BLIND75 | FREQUENT | STRIVER
}
