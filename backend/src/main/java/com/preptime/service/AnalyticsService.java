package com.preptime.service;

import com.preptime.model.*;
import com.preptime.repo.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    static final int WEAK_BELOW_PERCENT = 60;
    static final int MIN_ANSWERS = 3;

    private final AnswerRepo answers;
    private final AttemptRepo attempts;
    private final TopicRepo topics;
    private final SubjectRepo subjects;

    public AnalyticsService(AnswerRepo a, AttemptRepo at, TopicRepo t, SubjectRepo s) {
        this.answers = a; this.attempts = at; this.topics = t; this.subjects = s;
    }

    public Map<String, Object> compute(Long userId) {
        Map<Long, Topic> tm = topics.findAll().stream().collect(Collectors.toMap(t -> t.id, t -> t));
        Map<Long, String> sn = subjects.findAll().stream().collect(Collectors.toMap(s -> s.id, s -> s.name));
        Map<Long, int[]> agg = new LinkedHashMap<>();   // topicId -> [answered, correct]
        int totalAns = 0, totalCorrect = 0;
        for (AttemptAnswer a : answers.findByUserId(userId)) {
            int[] x = agg.computeIfAbsent(a.topicId, k -> new int[2]);
            x[0]++; totalAns++;
            if (a.correct) { x[1]++; totalCorrect++; }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var e : agg.entrySet()) {
            Topic t = tm.get(e.getKey());
            if (t == null) continue;
            int acc = (int) Math.round(100.0 * e.getValue()[1] / e.getValue()[0]);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("topicId", t.id);
            row.put("topicName", t.name);
            row.put("subjectName", sn.get(t.subjectId));
            row.put("answered", e.getValue()[0]);
            row.put("correct", e.getValue()[1]);
            row.put("accuracy", acc);
            row.put("weak", e.getValue()[0] >= MIN_ANSWERS && acc < WEAK_BELOW_PERCENT);
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt(r -> (Integer) r.get("accuracy")));

        List<QuizAttempt> recent = new ArrayList<>(attempts.findByUserIdOrderByTakenAtDesc(userId));
        int attemptCount = recent.size();
        if (recent.size() > 10) recent = recent.subList(0, 10);
        Collections.reverse(recent);
        List<Map<String, Object>> trend = new ArrayList<>();
        int i = attemptCount - recent.size();
        for (QuizAttempt q : recent) {
            i++;
            trend.add(Map.<String, Object>of("label", "#" + i,
                    "percent", q.total == 0 ? 0 : (int) Math.round(100.0 * q.score / q.total)));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attempts", attemptCount);
        out.put("answered", totalAns);
        out.put("overallAccuracy", totalAns == 0 ? null : (int) Math.round(100.0 * totalCorrect / totalAns));
        out.put("topics", rows);
        out.put("weakTopics", rows.stream().filter(r -> (Boolean) r.get("weak")).toList());
        out.put("trend", trend);
        return out;
    }

    /** Up to 3 weakest topic ids, used by the scheduler to add extra practice. */
    @SuppressWarnings("unchecked")
    public List<Long> weakTopicIds(Long userId) {
        return ((List<Map<String, Object>>) compute(userId).get("weakTopics")).stream()
                .limit(3).map(r -> (Long) r.get("topicId")).toList();
    }
}
