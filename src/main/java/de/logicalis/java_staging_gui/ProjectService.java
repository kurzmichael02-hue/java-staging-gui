package de.logicalis.java_staging_gui;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;

@Service
public class ProjectService {

    static final String SESSION_KEY = "projectTitle";

    @Autowired
    private ProjectRepository repo;

    public Project currentFromSession(HttpSession session) {
        String title = (String) session.getAttribute(SESSION_KEY);
        if (title == null) return null;
        return repo.findByProjectTitle(title).orElse(null);
    }

    public void setCurrent(HttpSession session, Project project) {
        session.setAttribute(SESSION_KEY, project.getProjectTitle());
    }

    public boolean exists(String title) {
        return repo.existsByProjectTitle(title);
    }

    // wizard step 1: if the project already exists, update the top-level meta and
    // return it. if not, create a new one. this way a second stager with
    // the same project name can simply attach to the existing config.
    @Transactional
    public Project createOrUpdate(String stagerName, String customer, String projectTitle, String orderNumber) {
        Optional<Project> existing = repo.findByProjectTitle(projectTitle);
        if (existing.isPresent()) {
            Project p = existing.get();
            // update top-level fields - only if the new input is not empty,
            // so an empty re-input does not accidentally delete anything
            if (stagerName != null && !stagerName.isBlank()) p.setStagerName(stagerName);
            if (customer != null && !customer.isBlank()) p.setCustomer(customer);
            if (orderNumber != null && !orderNumber.isBlank()) p.setOrderNumber(orderNumber);
            return repo.save(p);
        }
        return repo.save(new Project(stagerName, customer, projectTitle, orderNumber));
    }

    @Transactional
    public Project save(Project project) {
        return repo.save(project);
    }

    public List<Project> recentProjects() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    // clones config + network defaults from an existing project into a new one.
    // devices are NOT copied along (the new project starts empty), stager + order
    // come in fresh. saves retyping the network data when
    // multiple rollouts at the same customer share the same defaults.
    @Transactional
    public Project cloneFrom(String sourceTitle, String newTitle, String stagerName, String orderNumber) {
        Project src = repo.findByProjectTitle(sourceTitle).orElse(null);
        if (src == null) return null;

        Project p = new Project(stagerName, src.getCustomer(), newTitle, orderNumber);
        p.setVendor(src.getVendor());
        p.setModel(src.getModel());
        p.setConfigProfile(src.getConfigProfile());
        p.setDefaultGatewayIp(src.getDefaultGatewayIp());
        p.setLocalDnsServerIp(src.getLocalDnsServerIp());
        p.setLocalPrtgSensorIp(src.getLocalPrtgSensorIp());
        p.setLocalDhcpServerIps(src.getLocalDhcpServerIps());
        p.setNtpServer(src.getNtpServer());
        p.setSubnetMask(src.getSubnetMask());
        p.setSiteCountry(src.getSiteCountry());
        p.setSiteId(src.getSiteId());
        p.setTemplateFileName(src.getTemplateFileName());
        p.setIosxeVersion(src.getIosxeVersion());
        return repo.save(p);
    }
}
