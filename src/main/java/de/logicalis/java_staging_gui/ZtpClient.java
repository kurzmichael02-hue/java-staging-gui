package de.logicalis.java_staging_gui;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class ZtpClient {

    private static final Logger log = LoggerFactory.getLogger(ZtpClient.class);

    private final RestTemplate restTemplate;

    @Value("${ztp.server.baseurl}")
    private String baseUrl;

    public ZtpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendProvisioningData(Project project, Device device) {
        Map<String, Object> payload = buildPayload(project, device);
        String url = baseUrl + "/set_provisioning_data";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("ZTP {} -> {}", url, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            log.error("ZTP {} -> {} body={}", url, e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("ZTP {} failed", url, e);
            return false;
        }
    }

    private Map<String, Object> buildPayload(Project project, Device device) {
        Map<String, Object> general = new LinkedHashMap<>();
        general.put("ip_default_gateway", nz(project.getDefaultGatewayIp()));
        general.put("local_dns_server_ip_address", nz(project.getLocalDnsServerIp()));
        general.put("local_prtg_sensor_ip_address", nz(project.getLocalPrtgSensorIp()));
        general.put("local_dhcp_server_ip_addresses", nz(project.getLocalDhcpServerIps()));
        general.put("site_country", nz(project.getSiteCountry()));
        general.put("site_id", nz(project.getSiteId()));
        general.put("template_file_name", nz(project.getTemplateFileName()));
        general.put("software_version", nz(project.getIosxeVersion()));

        Map<String, Object> portEntry = new LinkedHashMap<>();
        portEntry.put("hostname", nz(device.getHostname()));
        portEntry.put("ip_address", nz(device.getIp()));
        portEntry.put("location_name", nz(device.getLocationName()));

        // the ztp server expects the structure { "<switch>": { "<port>": {...} } }
        // upstreamSwitch and upstreamPort are dynamic keys, so a map instead of a dto
        Map<String, Object> switchMap = new LinkedHashMap<>();
        switchMap.put(String.valueOf(device.getUpstreamPort()), portEntry);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("general", general);
        root.put(device.getUpstreamSwitch(), switchMap);
        return root;
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }
}
