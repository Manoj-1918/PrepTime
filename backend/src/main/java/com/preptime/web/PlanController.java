package com.preptime.web;

import com.preptime.model.*;
import com.preptime.service.PlanService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plan")
public class PlanController {
    record CreateReq(Integer totalDays, Double hoursPerDay, LocalDate startDate) {}
    record StatusReq(String status) {}

    private final PlanService service;

    public PlanController(PlanService service) { this.service = service; }

    @PostMapping
    public Map<String, Object> create(@RequestAttribute("userId") Long uid, @RequestBody CreateReq r) {
        StudyPlan p = service.create(uid, r.totalDays(), r.hoursPerDay(), r.startDate());
        service.refreshSummary(p.id, "created");
        Map<String, Object> out = service.current(uid);
        out.put("overflow", p.overflow);
        return out;
    }

    @GetMapping("/current")
    public Map<String, Object> current(@RequestAttribute("userId") Long uid) {
        return service.current(uid);
    }

    @PostMapping("/reschedule")
    public Map<String, Object> reschedule(@RequestAttribute("userId") Long uid) {
        Map<String, Object> r = service.reschedule(uid);
        if (((Number) r.get("missed")).intValue() > 0) service.refreshSummary((Long) r.get("planId"), "replanned");
        return r;
    }

    @PostMapping("/tasks/{id}/status")
    public Map<String, Object> status(@RequestAttribute("userId") Long uid, @PathVariable Long id, @RequestBody StatusReq r) {
        PlanTask t = service.setStatus(uid, id, r.status());
        return Map.<String, Object>of("id", t.id, "status", t.status);
    }
}
