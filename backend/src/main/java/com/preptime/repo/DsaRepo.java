package com.preptime.repo;

import com.preptime.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface DsaRepo extends JpaRepository<DsaProblem, Long> {
    List<DsaProblem> findByTopicId(Long topicId);
}
