package com.preptime.web;

import com.preptime.model.*;
import com.preptime.repo.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {
    record Ans(Long questionId, Integer selectedIndex) {}
    record Submit(Long topicId, List<Ans> answers) {}

    private final QuestionRepo questions;
    private final AttemptRepo attempts;
    private final AnswerRepo answers;

    public QuizController(QuestionRepo q, AttemptRepo a, AnswerRepo an) {
        this.questions = q; this.attempts = a; this.answers = an;
    }

    private static Map<String, Object> dto(Question q) {   // never sends the correct answer
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.id);
        m.put("topicId", q.topicId);
        m.put("text", q.questionText);
        m.put("options", q.options);
        m.put("difficulty", q.difficulty);
        return m;
    }

    @GetMapping("/topic/{topicId}")
    public List<Map<String, Object>> topicQuiz(@PathVariable Long topicId, @RequestParam(defaultValue = "10") int count) {
        List<Question> all = new ArrayList<>(questions.findByTopicId(topicId));
        Collections.shuffle(all);
        return all.stream().limit(Math.max(1, Math.min(count, 30))).map(QuizController::dto).toList();
    }

    @GetMapping("/mock")
    public List<Map<String, Object>> mock(@RequestParam(defaultValue = "15") int count) {
        List<Question> all = new ArrayList<>(questions.findAll());
        Collections.shuffle(all);
        return all.stream().limit(Math.max(1, Math.min(count, 50))).map(QuizController::dto).toList();
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestAttribute("userId") Long uid, @RequestBody Submit s) {
        if (s.answers() == null || s.answers().isEmpty()) throw new ApiException(400, "No answers submitted");
        Map<Long, Question> qs = questions.findAllById(s.answers().stream().map(Ans::questionId).toList())
                .stream().collect(Collectors.toMap(q -> q.id, q -> q));
        QuizAttempt at = new QuizAttempt();
        at.userId = uid;
        at.topicId = s.topicId();
        List<AttemptAnswer> rows = new ArrayList<>();
        List<Map<String, Object>> results = new ArrayList<>();
        int score = 0;
        for (Ans a : s.answers()) {
            Question q = qs.get(a.questionId());
            if (q == null) continue;
            int sel = a.selectedIndex() == null ? -1 : a.selectedIndex();
            boolean ok = sel == q.correctIndex;
            if (ok) score++;
            AttemptAnswer r = new AttemptAnswer();
            r.userId = uid; r.topicId = q.topicId; r.questionId = q.id; r.selectedIndex = sel; r.correct = ok;
            rows.add(r);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("questionId", q.id);
            m.put("selectedIndex", sel);
            m.put("correct", ok);
            m.put("correctIndex", q.correctIndex);
            m.put("explanation", q.explanation);
            results.add(m);
        }
        at.total = rows.size();
        at.score = score;
        attempts.save(at);
        rows.forEach(r -> r.attemptId = at.id);
        answers.saveAll(rows);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", at.id);
        out.put("score", score);
        out.put("total", rows.size());
        out.put("results", results);
        return out;
    }
}
