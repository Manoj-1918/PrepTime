package com.preptime.repo;

import com.preptime.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ArticleRepo extends JpaRepository<Article, Long> {
    List<Article> findByTopicId(Long topicId);
}
