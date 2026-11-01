package com.preptime.repo;

import com.preptime.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface AnswerRepo extends JpaRepository<AttemptAnswer, Long> {
    List<AttemptAnswer> findByUserId(Long userId);
}
