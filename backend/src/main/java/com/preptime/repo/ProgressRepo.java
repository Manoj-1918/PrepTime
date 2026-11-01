package com.preptime.repo;

import com.preptime.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProgressRepo extends JpaRepository<UserDsaProgress, Long> {
    List<UserDsaProgress> findByUserId(Long userId);
    Optional<UserDsaProgress> findByUserIdAndProblemId(Long userId, Long problemId);
}
