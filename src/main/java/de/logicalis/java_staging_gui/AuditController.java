package de.logicalis.java_staging_gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuditController {

    @Autowired
    private AuditService auditService;

    @Autowired
    private ProjectService projectService;

    @GetMapping("/audit")
    public String audit(HttpSession session, Model model) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        model.addAttribute("project", project);
        model.addAttribute("events", auditService.getEventsForProject(project.getProjectTitle()));
        return "audit";
    }
}
