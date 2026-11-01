package com.preptime.repo;

import com.preptime.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PlanRepo extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, String status);
    List<StudyPlan> findByUserIdAndStatus(Long userId, String status);
    List<StudyPlan> findByUserId(Long userId);
}
