package de.logicalis.java_staging_gui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping("/project")
    public String projectForm(Model model) {
        // existing project names as a hint in the input field, so a second
        // stager on the same rollout can attach via autocomplete instead of guessing
        model.addAttribute("recentProjects", projectService.recentProjects());
        return "project";
    }

    @PostMapping("/project")
    public String saveProject(
            @RequestParam String stagerName,
            @RequestParam String customer,
            @RequestParam String projectTitle,
            @RequestParam String orderNumber,
            HttpSession session,
            Model model) {

        // projectTitle becomes the DB key and the CSV file name, so trim and
        // check it. without trim "rollout " would count as a different project than "rollout".
        stagerName = stagerName.trim();
        customer = customer.trim();
        projectTitle = projectTitle.trim();
        orderNumber = orderNumber.trim();

        if (projectTitle.isEmpty() || projectTitle.length() > 100) {
            model.addAttribute("error", "Project title is missing or too long (max 100 characters).");
            model.addAttribute("recentProjects", projectService.recentProjects());
            return "project";
        }

        Project project = projectService.createOrUpdate(stagerName, customer, projectTitle, orderNumber);
        projectService.setCurrent(session, project);

        // if the project was already fully configured (defaults set), skip
        // the wizard and go straight to the dashboard. the new project flow goes through
        // /project-config + /project-defaults.
        if (project.getDefaultGatewayIp() != null && !project.getDefaultGatewayIp().isBlank()) {
            return "redirect:/devices";
        }
        return "redirect:/project-config";
    }

    @GetMapping("/project/clone")
    public String cloneForm(Model model) {
        model.addAttribute("projects", projectService.recentProjects());
        return "project-clone";
    }

    @PostMapping("/project/clone")
    public String cloneProject(
            @RequestParam String sourceTitle,
            @RequestParam String projectTitle,
            @RequestParam String stagerName,
            @RequestParam String orderNumber,
            HttpSession session,
            Model model) {

        sourceTitle = sourceTitle.trim();
        projectTitle = projectTitle.trim();
        stagerName = stagerName.trim();
        orderNumber = orderNumber.trim();

        if (projectTitle.isEmpty() || projectTitle.length() > 100) {
            model.addAttribute("error", "New project title is missing or too long (max 100 characters).");
            model.addAttribute("projects", projectService.recentProjects());
            return "project-clone";
        }
        if (projectService.exists(projectTitle)) {
            model.addAttribute("error", "A project with this title already exists.");
            model.addAttribute("projects", projectService.recentProjects());
            return "project-clone";
        }

        Project cloned = projectService.cloneFrom(sourceTitle, projectTitle, stagerName, orderNumber);
        if (cloned == null) {
            model.addAttribute("error", "Source project not found.");
            model.addAttribute("projects", projectService.recentProjects());
            return "project-clone";
        }

        projectService.setCurrent(session, cloned);
        // defaults are already there from the source project -> straight to device entry
        return "redirect:/device";
    }
}
