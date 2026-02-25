package de.logicalis.java_staging_gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProjectConfigController {

    @Autowired
    private ProjectService projectService;

    @GetMapping("/project-config")
    public String showProjectConfig(HttpSession session, Model model) {
        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        model.addAttribute("project", project);
        return "project-config";
    }

    @PostMapping("/project-config")
    public String saveProjectConfig(
            @RequestParam String vendor,
            @RequestParam String modelName,
            @RequestParam String configProfile,
            HttpSession session,
            Model model) {

        Project project = projectService.currentFromSession(session);
        if (project == null) return "redirect:/project";

        if (vendor == null || vendor.trim().equals("")
                || modelName == null || modelName.trim().equals("")
                || configProfile == null || configProfile.trim().equals("")) {

            model.addAttribute("project", project);
            model.addAttribute("error", "Please fill in all required fields.");
            return "project-config";
        }

        project.setVendor(vendor.trim());
        project.setModel(modelName.trim());
        project.setConfigProfile(configProfile.trim());

        projectService.save(project);

        return "redirect:/project-defaults";
    }
}
