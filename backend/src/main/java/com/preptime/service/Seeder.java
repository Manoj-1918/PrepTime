package com.preptime.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preptime.model.*;
import com.preptime.repo.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Merges resources/seed/*.json into the database on every start (adds new, updates edited, never deletes). */
@Component
public class Seeder implements CommandLineRunner {
    static final String[] FILES = {"aptitude", "dsa", "dbms", "os", "cn", "system-design"};

    private final SubjectRepo subjects;
    private final TopicRepo topics;
    private final ArticleRepo articles;
    private final QuestionRepo questions;
    private final DsaRepo dsa;
    private final ObjectMapper mapper;

    public Seeder(SubjectRepo s, TopicRepo t, ArticleRepo a, QuestionRepo q, DsaRepo d, ObjectMapper m) {
        this.subjects = s; this.topics = t; this.articles = a; this.questions = q; this.dsa = d; this.mapper = m;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        Map<String, Subject> subs = subjects.findAll().stream().collect(Collectors.toMap(s -> s.name, s -> s, (a, b) -> a));
        Map<String, Topic> tops = topics.findAll().stream().collect(Collectors.toMap(t -> t.subjectId + "|" + t.name, t -> t, (a, b) -> a));
        Map<String, Article> arts = articles.findAll().stream().collect(Collectors.toMap(a -> a.topicId + "|" + a.title, a -> a, (a, b) -> a));
        Map<String, Question> qs = questions.findAll().stream().collect(Collectors.toMap(q -> q.topicId + "|" + q.questionText, q -> q, (a, b) -> a));
        Map<String, DsaProblem> probs = dsa.findAll().stream().collect(Collectors.toMap(p -> p.topicId + "|" + p.title, p -> p, (a, b) -> a));
        int added = 0;

        for (String f : FILES) {
            JsonNode n = mapper.readTree(new ClassPathResource("seed/" + f + ".json").getInputStream());
            Subject s = subs.getOrDefault(n.get("name").asText(), new Subject());
            s.name = n.get("name").asText();
            s.category = n.get("category").asText();
            s.description = n.path("description").asText("");
            s.weight = n.path("weight").asInt(10);
            s.sortOrder = n.path("sortOrder").asInt(0);
            subjects.save(s);
            subs.put(s.name, s);

            int order = 0;
            for (JsonNode tn : n.get("topics")) {
                String tname = tn.get("name").asText();
                Topic t = tops.getOrDefault(s.id + "|" + tname, new Topic());
                if (t.id == null) added++;
                t.subjectId = s.id;
                t.name = tname;
                t.estHours = tn.path("estHours").asDouble(3);
                t.sortOrder = order++;
                topics.save(t);
                tops.put(s.id + "|" + tname, t);

                for (JsonNode an : tn.path("articles")) {
                    String key = t.id + "|" + an.get("title").asText();
                    Article a = arts.getOrDefault(key, new Article());
                    if (a.id == null) added++;
                    a.topicId = t.id;
                    a.title = an.get("title").asText();
                    a.content = an.get("content").asText();
                    a.type = an.path("type").asText("CONCEPT");
                    articles.save(a);
                    arts.put(key, a);
                }
                for (JsonNode qn : tn.path("questions")) {
                    String key = t.id + "|" + qn.get("q").asText();
                    Question q = qs.getOrDefault(key, new Question());
                    if (q.id == null) added++;
                    q.topicId = t.id;
                    q.questionText = qn.get("q").asText();
                    q.options = new ArrayList<>();
                    for (JsonNode o : qn.get("options")) q.options.add(o.asText());
                    q.correctIndex = qn.get("answer").asInt();
                    q.explanation = qn.path("explanation").asText("");
                    q.difficulty = qn.path("difficulty").asText("MEDIUM");
                    questions.save(q);
                    qs.put(key, q);
                }
                for (JsonNode pn : tn.path("problems")) {
                    String key = t.id + "|" + pn.get("title").asText();
                    DsaProblem p = probs.getOrDefault(key, new DsaProblem());
                    if (p.id == null) added++;
                    p.topicId = t.id;
                    p.title = pn.get("title").asText();
                    p.url = "https://leetcode.com/problems/" + pn.get("slug").asText() + "/";
                    p.difficulty = pn.path("difficulty").asText("MEDIUM");
                    p.sheet = pn.path("sheet").asText("BLIND75");
                    dsa.save(p);
                    probs.put(key, p);
                }
            }
        }
        System.out.println("PrepTime: seed merged, " + added + " new items.");
    }
}