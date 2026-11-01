package com.preptime.web;

import com.preptime.repo.*;
import com.preptime.service.AnalyticsService;
import com.preptime.service.PlanService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final PlanService plans;
    private final AnalyticsService analytics;
    private final ProgressRepo progress;
    private final DsaRepo dsa;

    public DashboardController(PlanService plans, AnalyticsService analytics, ProgressRepo progress, DsaRepo dsa) {
        this.plans = plans; this.analytics = analytics; this.progress = progress; this.dsa = dsa;
    }

    @GetMapping("/dashboard")
    @SuppressWarnings("unchecked")
    public Map<String, Object> dashboard(@RequestAttribute("userId") Long uid) {
        Map<String, Object> out = plans.current(uid);
        out.put("hasPlan", out.get("plan") != null);
        if (out.get("plan") != null) {
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) out.remove("tasks");
            String today = (String) out.get("today");
            out.put("todayTasks", tasks.stream().filter(t -> today.equals(t.get("date"))).toList());
            int doneMin = 0, totalMin = 0, done = 0;
            for (Map<String, Object> t : tasks) {
                int m = (Integer) t.get("minutes");
                totalMin += m;
                if ("DONE".equals(t.get("status"))) { doneMin += m; done++; }
            }
            out.put("progress", Map.<String, Object>of("done", done, "total", tasks.size(),
                    "percent", totalMin == 0 ? 0 : Math.round(100.0 * doneMin / totalMin)));
        }
        Map<String, Object> a = analytics.compute(uid);
        out.put("accuracy", a.get("overallAccuracy"));
        out.put("weakTopics", a.get("weakTopics"));
        out.put("streak", plans.streak(uid));
        out.put("dsaSolved", progress.findByUserId(uid).size());
        out.put("dsaTotal", dsa.count());
        return out;
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics(@RequestAttribute("userId") Long uid) {
        return analytics.compute(uid);
    }
}
