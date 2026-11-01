package com.preptime.service;

import static java.util.Comparator.comparingInt;

import com.preptime.model.*;
import com.preptime.repo.*;
import com.preptime.web.ApiException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The scheduling engine.
 *  - create():     spreads every topic over the available days (rule based, DSA gets the biggest share)
 *  - reschedule(): when days were missed, moves unfinished work onto the remaining days
 * Gemini is only used afterwards to write the human-friendly summary (see refreshSummary).
 */
@Service
public class PlanService {
    record Draft(Long topicId, String type, String title, int minutes) {}

    private final PlanRepo plans;
    private final TaskRepo tasks;
    private final TopicRepo topics;
    private final SubjectRepo subjects;
    private final AnalyticsService analytics;
    private final AiClient ai;

    public PlanService(PlanRepo plans, TaskRepo tasks, TopicRepo topics, SubjectRepo subjects,
                       AnalyticsService analytics, AiClient ai) {
        this.plans = plans; this.tasks = tasks; this.topics = topics; this.subjects = subjects;
        this.analytics = analytics; this.ai = ai;
    }

    /** Every 7th day and the last day are revision + mock-test days. */
    static boolean isRevisionDay(StudyPlan p, long dayNo) {
        return p.totalDays >= 7 && (dayNo % 7 == 0 || dayNo == p.totalDays);
    }

    // ------------------------------------------------------------------ create
    @Transactional
    public StudyPlan create(Long uid, Integer days, Double hours, LocalDate start) {
        int d = days == null ? 90 : days;
        double h = hours == null ? 3 : hours;
        if (d < 7 || d > 365) throw new ApiException(400, "Choose between 7 and 365 days");
        if (h < 0.5 || h > 16) throw new ApiException(400, "Choose between 0.5 and 16 hours per day");
        for (StudyPlan old : plans.findByUserIdAndStatus(uid, "ACTIVE")) { old.status = "ARCHIVED"; plans.save(old); }

        StudyPlan p = new StudyPlan();
        p.userId = uid;
        p.totalDays = d;
        p.hoursPerDay = h;
        p.startDate = start == null ? LocalDate.now() : start;
        plans.save(p);
        p.overflow = place(p, new ArrayDeque<>(buildDrafts(p)), p.startDate, 0, Set.of(), "", 0);
        return p;
    }

    private List<Draft> buildDrafts(StudyPlan p) {
        List<Subject> subs = new ArrayList<>(subjects.findAll());
        subs.sort(comparingInt(s -> s.sortOrder));
        Map<Long, List<Topic>> by = topics.findAll().stream().collect(Collectors.groupingBy(t -> t.subjectId));
        Map<Long, Deque<Topic>> queues = new HashMap<>();
        double sum = 0;
        for (Subject s : subs) {
            List<Topic> l = by.getOrDefault(s.id, List.of()).stream().sorted(comparingInt(t -> t.sortOrder)).toList();
            queues.put(s.id, new ArrayDeque<>(l));
            for (Topic t : l) sum += t.estHours * 60;
        }
        if (sum == 0) throw new ApiException(500, "No study content is loaded yet");

        int revDays = 0;
        for (long i = 1; i <= p.totalDays; i++) if (isRevisionDay(p, i)) revDays++;
        double available = (p.totalDays - revDays) * p.hoursPerDay * 60;
        double scale = Math.max(0.25, Math.min(3.0, available / sum));   // more days => more practice per topic

        // "Stride" ordering: subjects with a bigger weight (DSA) come up more often.
        Map<Long, Double> pass = new HashMap<>();
        List<Draft> out = new ArrayList<>();
        while (true) {
            Subject best = null;
            for (Subject s : subs) {
                if (queues.get(s.id).isEmpty()) continue;
                if (best == null || pass.getOrDefault(s.id, 0.0) < pass.getOrDefault(best.id, 0.0)) best = s;
            }
            if (best == null) break;
            Topic t = queues.get(best.id).poll();
            pass.merge(best.id, t.estHours * 60 / Math.max(1, best.weight), Double::sum);
            double m = Math.max(30, t.estHours * 60 * scale);
            if ("DSA".equals(best.category)) {
                out.add(draft(t, "READ", "Read: ", m * 0.25));
                out.add(draft(t, "PRACTICE", "Practice problems: ", m * 0.55));
                out.add(draft(t, "QUIZ", "Quiz: ", m * 0.20));
            } else {
                out.add(draft(t, "READ", "Read: ", m * 0.55));
                out.add(draft(t, "QUIZ", "Quiz: ", m * 0.45));
            }
        }
        return out;
    }

    private static Draft draft(Topic t, String type, String prefix, double minutes) {
        int m = Math.max(10, (int) (Math.round(minutes / 5.0) * 5));
        return new Draft(t.id, type, prefix + t.name, m);
    }

    /** Puts drafts on days from..end. Returns how many drafts did not fit. */
    private int place(StudyPlan p, Deque<Draft> q, LocalDate from, int usedOnFirstDay,
                      Set<LocalDate> skipRevision, String focus, int orderStart) {
        int cap = (int) Math.round(p.hoursPerDay * 60);
        LocalDate end = p.startDate.plusDays(p.totalDays - 1L);
        int order = orderStart;
        List<PlanTask> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(end); d = d.plusDays(1)) {
            long dayNo = ChronoUnit.DAYS.between(p.startDate, d) + 1;
            if (isRevisionDay(p, dayNo)) {
                if (!skipRevision.contains(d)) {
                    String title = (dayNo == p.totalDays ? "Final mock test & revision" : "Weekly revision & mock test") + focus;
                    out.add(task(p, d, null, "REVISION", title, cap, order++));
                }
                continue;
            }
            int left = cap - (d.equals(from) ? usedOnFirstDay : 0);
            while (left >= 10 && !q.isEmpty()) {
                Draft x = q.pollFirst();
                if (x.minutes() <= left) {
                    out.add(task(p, d, x.topicId(), x.type(), x.title(), x.minutes(), order++));
                    left -= x.minutes();
                } else {   // split a long task across two days
                    out.add(task(p, d, x.topicId(), x.type(), x.title(), left, order++));
                    String title = x.title().endsWith(" (cont.)") ? x.title() : x.title() + " (cont.)";
                    q.addFirst(new Draft(x.topicId(), x.type(), title, x.minutes() - left));
                    left = 0;
                }
            }
        }
        tasks.saveAll(out);
        return q.size();
    }

    private static PlanTask task(StudyPlan p, LocalDate d, Long topicId, String type, String title, int minutes, int order) {
        PlanTask t = new PlanTask();
        t.planId = p.id; t.taskDate = d; t.topicId = topicId; t.taskType = type;
        t.title = title; t.minutes = minutes; t.sortOrder = order;
        return t;
    }

    // -------------------------------------------------------------- reschedule
    @Transactional
    public Map<String, Object> reschedule(Long uid) {
        StudyPlan p = active(uid);
        LocalDate today = LocalDate.now();
        LocalDate end = p.startDate.plusDays(p.totalDays - 1L);
        if (today.isAfter(end)) throw new ApiException(400, "This plan has ended. Create a new plan to keep going.");

        List<PlanTask> all = tasks.findByPlanIdOrderByTaskDateAscSortOrderAsc(p.id);
        for (PlanTask t : all) if ("PENDING".equals(t.status) && t.taskDate.isBefore(today)) t.status = "MISSED";

        List<Draft> carry = new ArrayList<>();
        List<PlanTask> changed = new ArrayList<>();
        int missed = 0;
        for (PlanTask t : all) {                       // 1) missed work goes first
            if ("MISSED".equals(t.status)) {
                missed++;
                if (!"REVISION".equals(t.taskType)) carry.add(new Draft(t.topicId, t.taskType, t.title, t.minutes));
                t.status = "RESCHEDULED";
                changed.add(t);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("planId", p.id);
        res.put("missed", missed);
        if (missed == 0) {
            res.put("moved", 0); res.put("overflow", 0); res.put("version", p.version);
            res.put("message", "You are on track. Nothing to reschedule.");
            return res;
        }
        List<PlanTask> toDelete = new ArrayList<>();
        for (PlanTask t : all) {                       // 2) then everything still ahead
            if ("PENDING".equals(t.status) && !t.taskDate.isBefore(today)) {
                if (!"REVISION".equals(t.taskType)) carry.add(new Draft(t.topicId, t.taskType, t.title, t.minutes));
                toDelete.add(t);
            }
        }
        tasks.saveAll(changed);
        tasks.deleteAll(toDelete);

        // 3) weak topics get one extra short quiz
        Map<Long, Topic> tm = topics.findAll().stream().collect(Collectors.toMap(t -> t.id, t -> t));
        Set<String> titles = carry.stream().map(Draft::title).collect(Collectors.toCollection(HashSet::new));
        List<String> weakNames = new ArrayList<>();
        for (Long id : analytics.weakTopicIds(uid)) {
            Topic t = tm.get(id);
            if (t == null) continue;
            weakNames.add(t.name);
            String title = "Re-quiz (weak area): " + t.name;
            if (titles.add(title)) carry.add(0, new Draft(id, "QUIZ", title, 20));
        }

        // 4) fit into the remaining study capacity
        LocalDate from = today.isBefore(p.startDate) ? p.startDate : today;
        int cap = (int) Math.round(p.hoursPerDay * 60);
        int used = 0;
        Set<LocalDate> skipRev = new HashSet<>();
        for (PlanTask t : all) {
            if ("DONE".equals(t.status) && t.taskDate.equals(from)) used += t.minutes;
            if ("DONE".equals(t.status) && "REVISION".equals(t.taskType)) skipRev.add(t.taskDate);
        }
        int capacity = -used;
        for (LocalDate d = from; !d.isAfter(end); d = d.plusDays(1))
            if (!isRevisionDay(p, ChronoUnit.DAYS.between(p.startDate, d) + 1)) capacity += cap;
        int total = carry.stream().mapToInt(Draft::minutes).sum();
        List<Draft> fitted = carry;
        if (total > capacity && capacity > 0) {
            final double f = (double) capacity / total;
            fitted = carry.stream()
                    .map(x -> new Draft(x.topicId(), x.type(), x.title(), Math.max(10, (int) (Math.round(x.minutes() * f / 5.0) * 5))))
                    .toList();
        }
        String focus = weakNames.isEmpty() ? "" : " (focus: " + String.join(", ", weakNames) + ")";
        int overflow = place(p, new ArrayDeque<>(fitted), from, used, skipRev, focus, 100);

        p.version++;
        plans.save(p);
        res.put("moved", carry.size());
        res.put("overflow", overflow);
        res.put("version", p.version);
        res.put("message", missed + " missed task" + (missed == 1 ? "" : "s") + " moved. Your remaining plan was re-spread over the days left.");
        return res;
    }

    // -------------------------------------------------------------------- AI summary
    /** Not transactional on purpose: the Gemini call can be slow. */
    public void refreshSummary(Long planId, String reason) {
        StudyPlan p = plans.findById(planId).orElse(null);
        if (p == null) return;
        List<PlanTask> all = tasks.findByPlanIdOrderByTaskDateAscSortOrderAsc(planId);
        Map<Long, Topic> tm = topics.findAll().stream().collect(Collectors.toMap(t -> t.id, t -> t));
        List<String> order = all.stream().filter(t -> t.topicId != null && "READ".equals(t.taskType) && tm.containsKey(t.topicId))
                .map(t -> tm.get(t.topicId).name).distinct().limit(14).toList();
        List<String> weak = analytics.weakTopicIds(p.userId).stream().filter(tm::containsKey).map(id -> tm.get(id).name).toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("totalDays", p.totalDays);
        payload.put("hoursPerDay", p.hoursPerDay);
        payload.put("topicOrder", order);
        payload.put("weakTopics", weak);
        payload.put("reason", reason);
        String s = ai.planSummary(payload);
        if (s == null || s.isBlank()) {
            s = "Your " + p.totalDays + "-day plan uses about " + p.hoursPerDay + " hours a day. DSA gets the biggest share, "
                    + "every 7th day is revision with a mock test, and the last day is a final mock. "
                    + ("created".equals(reason) ? "If you miss a day, PrepTime re-spreads the remaining work for you."
                    : "Your missed tasks were moved onto upcoming days.");
        }
        p.aiSummary = s;
        plans.save(p);
    }

    // ------------------------------------------------------------------ task status
    public PlanTask setStatus(Long uid, Long taskId, String status) {
        if (!"DONE".equals(status) && !"PENDING".equals(status)) throw new ApiException(400, "Status must be DONE or PENDING");
        PlanTask t = tasks.findById(taskId).orElseThrow(() -> new ApiException(404, "Task not found"));
        StudyPlan p = plans.findById(t.planId).orElseThrow(() -> new ApiException(404, "Task not found"));
        if (!p.userId.equals(uid)) throw new ApiException(404, "Task not found");
        t.status = status;
        t.completedAt = "DONE".equals(status) ? LocalDateTime.now() : null;
        return tasks.save(t);
    }

    // ------------------------------------------------------------------ read side
    public StudyPlan active(Long uid) {
        return plans.findFirstByUserIdAndStatusOrderByIdDesc(uid, "ACTIVE")
                .orElseThrow(() -> new ApiException(404, "You do not have a study plan yet"));
    }

    public List<Map<String, Object>> views(List<PlanTask> ts) {
        Map<Long, Topic> tm = topics.findAll().stream().collect(Collectors.toMap(t -> t.id, t -> t));
        return ts.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.id);
            m.put("date", t.taskDate.toString());
            m.put("topicId", t.topicId);
            m.put("topicName", t.topicId != null && tm.containsKey(t.topicId) ? tm.get(t.topicId).name : null);
            m.put("type", t.taskType);
            m.put("title", t.title);
            m.put("minutes", t.minutes);
            m.put("status", t.status);
            return m;
        }).toList();
    }

    public Map<String, Object> current(Long uid) {
        Map<String, Object> r = new LinkedHashMap<>();
        StudyPlan p = plans.findFirstByUserIdAndStatusOrderByIdDesc(uid, "ACTIVE").orElse(null);
        r.put("plan", p);
        if (p == null) return r;
        List<PlanTask> all = tasks.findByPlanIdOrderByTaskDateAscSortOrderAsc(p.id);
        LocalDate today = LocalDate.now();
        r.put("today", today.toString());
        r.put("endDate", p.startDate.plusDays(p.totalDays - 1L).toString());
        r.put("dayNumber", Math.max(1, Math.min(p.totalDays, ChronoUnit.DAYS.between(p.startDate, today) + 1)));
        r.put("missedCount", all.stream().filter(t -> "MISSED".equals(t.status)
                || ("PENDING".equals(t.status) && t.taskDate.isBefore(today))).count());
        r.put("tasks", views(all.stream().filter(t -> !"RESCHEDULED".equals(t.status)).toList()));
        r.put("strip", strip(p, all, today));
        return r;
    }

    private List<Map<String, Object>> strip(StudyPlan p, List<PlanTask> all, LocalDate today) {
        Map<LocalDate, List<PlanTask>> by = all.stream().collect(Collectors.groupingBy(t -> t.taskDate));
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < p.totalDays; i++) {
            LocalDate d = p.startDate.plusDays(i);
            List<PlanTask> ts = by.getOrDefault(d, List.of());
            boolean allDone = !ts.isEmpty() && ts.stream().allMatch(t -> "DONE".equals(t.status));
            String state;
            if (allDone) state = "done";
            else if (d.isBefore(today)) state = ts.isEmpty() ? "off" : "missed";
            else if (d.equals(today)) state = "today";
            else state = "upcoming";
            out.add(Map.<String, Object>of("day", i + 1, "date", d.toString(), "state", state,
                    "revision", ts.stream().anyMatch(t -> "REVISION".equals(t.taskType))));
        }
        return out;
    }

    /** Consecutive days (ending today or yesterday) with at least one completed task. */
    public int streak(Long uid) {
        List<Long> ids = plans.findByUserId(uid).stream().map(p -> p.id).toList();
        if (ids.isEmpty()) return 0;
        Set<LocalDate> days = tasks.findByPlanIdIn(ids).stream().filter(t -> t.completedAt != null)
                .map(t -> t.completedAt.toLocalDate()).collect(Collectors.toSet());
        LocalDate d = LocalDate.now();
        if (!days.contains(d)) d = d.minusDays(1);
        int n = 0;
        while (days.contains(d)) { n++; d = d.minusDays(1); }
        return n;
    }
}
