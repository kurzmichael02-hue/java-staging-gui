package de.logicalis.java_staging_gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private ProjectService projectService;

    @GetMapping("/metrics")
    public String metrics(HttpSession session, Model model) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        ProjectMetrics metrics = metricsService.computeFor(project.getProjectTitle());
        model.addAttribute("project", project);
        model.addAttribute("metrics", metrics);
        model.addAttribute("stagers", metricsService.stagerStatsFor(project.getProjectTitle()));
        return "metrics";
    }
}
