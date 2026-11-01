package com.preptime.web;

import static java.util.Comparator.comparingInt;

import com.preptime.model.*;
import com.preptime.repo.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ContentController {
    private final SubjectRepo subjects;
    private final TopicRepo topics;
    private final ArticleRepo articles;
    private final QuestionRepo questions;
    private final DsaRepo dsa;
    private final ProgressRepo progress;

    public ContentController(SubjectRepo s, TopicRepo t, ArticleRepo a, QuestionRepo q, DsaRepo d, ProgressRepo p) {
        this.subjects = s; this.topics = t; this.articles = a; this.questions = q; this.dsa = d; this.progress = p;
    }

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects(@RequestAttribute("userId") Long uid) {
        Map<Long, Long> qCount = questions.findAll().stream().collect(Collectors.groupingBy(q -> q.topicId, Collectors.counting()));
        Map<Long, Long> pCount = dsa.findAll().stream().collect(Collectors.groupingBy(p -> p.topicId, Collectors.counting()));
        Set<Long> solved = progress.findByUserId(uid).stream().map(p -> p.problemId).collect(Collectors.toSet());
        Map<Long, Long> solvedByTopic = dsa.findAll().stream().filter(p -> solved.contains(p.id))
                .collect(Collectors.groupingBy(p -> p.topicId, Collectors.counting()));
        Map<Long, List<Topic>> byS = topics.findAll().stream().collect(Collectors.groupingBy(t -> t.subjectId));
        List<Subject> ss = new ArrayList<>(subjects.findAll());
        ss.sort(comparingInt(s -> s.sortOrder));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Subject s : ss) {
            List<Map<String, Object>> ts = new ArrayList<>();
            for (Topic t : byS.getOrDefault(s.id, List.of()).stream().sorted(comparingInt(x -> x.sortOrder)).toList()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.id);
                m.put("name", t.name);
                m.put("estHours", t.estHours);
                m.put("questionCount", qCount.getOrDefault(t.id, 0L));
                m.put("problemCount", pCount.getOrDefault(t.id, 0L));
                m.put("solvedCount", solvedByTopic.getOrDefault(t.id, 0L));
                ts.add(m);
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.id);
            m.put("name", s.name);
            m.put("category", s.category);
            m.put("description", s.description);
            m.put("topics", ts);
            out.add(m);
        }
        return out;
    }

    @GetMapping("/topics/{id}")
    public Map<String, Object> topic(@PathVariable Long id) {
        Topic t = topics.findById(id).orElseThrow(() -> new ApiException(404, "Topic not found"));
        Subject s = subjects.findById(t.subjectId).orElseThrow(() -> new ApiException(404, "Topic not found"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.id);
        m.put("name", t.name);
        m.put("subjectName", s.name);
        m.put("category", s.category);
        m.put("estHours", t.estHours);
        m.put("articles", articles.findByTopicId(id));
        m.put("questionCount", questions.findByTopicId(id).size());
        m.put("problemCount", dsa.findByTopicId(id).size());
        return m;
    }

    @GetMapping("/topics/{id}/dsa")
    public List<Map<String, Object>> problems(@RequestAttribute("userId") Long uid, @PathVariable Long id) {
        Set<Long> solved = progress.findByUserId(uid).stream().map(p -> p.problemId).collect(Collectors.toSet());
        return dsa.findByTopicId(id).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id);
            m.put("title", p.title);
            m.put("url", p.url);
            m.put("difficulty", p.difficulty);
            m.put("sheet", p.sheet);
            m.put("solved", solved.contains(p.id));
            return m;
        }).toList();
    }

    @PostMapping("/dsa/{problemId}/toggle")
    public Map<String, Object> toggle(@RequestAttribute("userId") Long uid, @PathVariable Long problemId) {
        if (!dsa.existsById(problemId)) throw new ApiException(404, "Problem not found");
        var existing = progress.findByUserIdAndProblemId(uid, problemId);
        if (existing.isPresent()) {
            progress.delete(existing.get());
            return Map.of("solved", false);
        }
        UserDsaProgress p = new UserDsaProgress();
        p.userId = uid;
        p.problemId = problemId;
        progress.save(p);
        return Map.of("solved", true);
    }
}
