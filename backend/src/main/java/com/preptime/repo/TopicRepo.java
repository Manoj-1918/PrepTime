package com.preptime.repo;

import com.preptime.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TopicRepo extends JpaRepository<Topic, Long> {
    List<Topic> findBySubjectIdOrderBySortOrder(Long subjectId);
}
