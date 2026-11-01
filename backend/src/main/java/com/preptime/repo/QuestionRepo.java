package com.preptime.repo;

import com.preptime.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface QuestionRepo extends JpaRepository<Question, Long> {
    List<Question> findByTopicId(Long topicId);
}
