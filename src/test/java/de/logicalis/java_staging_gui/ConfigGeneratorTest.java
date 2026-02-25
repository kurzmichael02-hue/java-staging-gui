package de.logicalis.java_staging_gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ConfigGeneratorTest {

    private ConfigGenerator gen;

    @BeforeEach
    void setUp() {
        gen = new ConfigGenerator();
        // set the @Value fields by hand so no spring context is needed
        ReflectionTestUtils.setField(gen, "adminPassword", "TestPass123!");
        ReflectionTestUtils.setField(gen, "snmpCommunity", "test-community");
    }

    @Test
    void profilS_enthaeltHostnameGatewayDnsSshAberKeineAaa() {
        String config = gen.generate(makeDevice("S"));

        assertThat(config).contains("hostname sw-test-01");
        assertThat(config).contains("ip default-gateway 10.0.0.1");
        assertThat(config).contains("ip name-server 10.0.0.53");
        assertThat(config).contains("ip ssh version 2");
        assertThat(config).contains("secret TestPass123!");

        assertThat(config).doesNotContain("aaa new-model");
        assertThat(config).doesNotContain("snmp-server");
        assertThat(config).doesNotContain("dhcp snooping");
    }

    @Test
    void profilM_enthaeltUplinkUndPortfast() {
        String config = gen.generate(makeDevice("M"));

        assertThat(config).contains("interface GigabitEthernet1/0/5");
        assertThat(config).contains("switchport mode trunk");
        assertThat(config).contains("spanning-tree portfast trunk");
        assertThat(config).contains("UPLINK-TO-SW-FRA-01");

        assertThat(config).doesNotContain("aaa new-model");
    }

    @Test
    void profilL_enthaeltAaaSnmpDhcpSnoopingNtpBanner() {
        String config = gen.generate(makeDevice("L"));

        assertThat(config).contains("aaa new-model");
        assertThat(config).contains("aaa authentication login default local");
        assertThat(config).contains("snmp-server community test-community RO");
        assertThat(config).contains("ip dhcp snooping");
        assertThat(config).contains("ntp server 10.0.0.123");
        assertThat(config).contains("banner motd");
        assertThat(config).contains("TEST-LOGICALIS");

        // L uses 'login authentication default' instead of 'login local'
        assertThat(config).contains("login authentication default");
        assertThat(config).doesNotContain("login local");
    }

    @Test
    void unbekanntesProfil_faelltAufSZurueck() {
        String config = gen.generate(makeDevice("XYZ"));

        assertThat(config).contains("hostname sw-test-01");
        assertThat(config).doesNotContain("aaa new-model");
    }

    @Test
    void nullProfil_faelltAufSZurueck() {
        String config = gen.generate(makeDevice(null));

        assertThat(config).contains("hostname sw-test-01");
        assertThat(config).doesNotContain("aaa new-model");
    }

    @Test
    void adminPasswordKommtAusProperty() {
        String config = gen.generate(makeDevice("L"));

        // the value comes from the test property, not from a literal in the code
        assertThat(config).contains("secret TestPass123!");
    }

    @Test
    void snmpCommunityKommtAusProperty() {
        String config = gen.generate(makeDevice("L"));

        assertThat(config).contains("snmp-server community test-community RO");
        assertThat(config).doesNotContain("snmp-server community public RO");
    }

    private Device makeDevice(String profile) {
        return new Device(
                "sw-fra-01", 5,
                "sw-test-01", "10.0.0.10", "FCW001",
                "Frankfurt", "rollout-test",
                "Cisco", "C9200",
                profile,
                "AUTO", null,
                "10.0.0.1", "10.0.0.53",
                "10.0.0.100", "10.0.0.123",
                "255.255.255.0", "Michael");
    }
}
