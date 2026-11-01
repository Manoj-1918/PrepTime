package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long topicId;
    public String title;
    @Column(columnDefinition = "text") public String content;   // markdown
    public String type;                                           // CONCEPT | FORMULA | SYSTEM_DESIGN
}
