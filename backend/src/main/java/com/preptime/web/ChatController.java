package com.preptime.web;

import com.preptime.model.*;
import com.preptime.repo.*;
import com.preptime.service.AiClient;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    record Msg(String role, String content) {}
    record ChatReq(Long topicId, String message, List<Msg> history) {}

    private final AiClient ai;
    private final TopicRepo topics;
    private final SubjectRepo subjects;

    public ChatController(AiClient ai, TopicRepo topics, SubjectRepo subjects) {
        this.ai = ai; this.topics = topics; this.subjects = subjects;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody ChatReq r) {
        if (r.message() == null || r.message().isBlank()) throw new ApiException(400, "Type a question first");
        if (r.message().length() > 1000) throw new ApiException(400, "Keep the question under 1000 characters");
        String topic = "general placement preparation", subject = "Computer Science";
        if (r.topicId() != null) {
            Topic t = topics.findById(r.topicId()).orElse(null);
            if (t != null) {
                topic = t.name;
                subject = subjects.findById(t.subjectId).map(s -> s.name).orElse(subject);
            }
        }
        List<Map<String, String>> history = new ArrayList<>();
        if (r.history() != null) {
            List<Msg> h = r.history().size() > 6 ? r.history().subList(r.history().size() - 6, r.history().size()) : r.history();
            for (Msg m : h) history.add(Map.of("role", "user".equals(m.role()) ? "user" : "ai", "content", m.content() == null ? "" : m.content()));
        }
        return Map.of("answer", ai.chat(topic, subject, r.message(), history));
    }
}
