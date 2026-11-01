package com.preptime.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long topicId;
    @Column(columnDefinition = "text") public String questionText;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") public List<String> options;
    public int correctIndex;
    @Column(columnDefinition = "text") public String explanation;
    public String difficulty;
}
