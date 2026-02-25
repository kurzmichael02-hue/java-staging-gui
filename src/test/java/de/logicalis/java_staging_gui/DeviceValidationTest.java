package de.logicalis.java_staging_gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeviceValidationTest {

    @Test
    void allesGueltig_returntNull() {
        assertThat(DeviceValidation.validate(
                "sw-fra-01", 5, "sw-test", "10.0.0.1", "Frankfurt"))
                .isNull();
    }

    @Test
    void leererUpstreamSwitch_meldetSwitch() {
        assertThat(DeviceValidation.validate("", 5, "sw-test", "10.0.0.1", "Frankfurt"))
                .contains("Upstream Switch");
    }

    @Test
    void nullUpstreamSwitch_meldetSwitch() {
        assertThat(DeviceValidation.validate(null, 5, "sw-test", "10.0.0.1", "Frankfurt"))
                .contains("Upstream Switch");
    }

    @Test
    void portKleiner1_meldetPort() {
        assertThat(DeviceValidation.validate("sw", 0, "sw-test", "10.0.0.1", "Frankfurt"))
                .contains("Upstream Port");
    }

    @Test
    void portGroesser48_meldetPort() {
        assertThat(DeviceValidation.validate("sw", 49, "sw-test", "10.0.0.1", "Frankfurt"))
                .contains("Upstream Port");
    }

    @Test
    void nullPort_meldetPort() {
        assertThat(DeviceValidation.validate("sw", null, "sw-test", "10.0.0.1", "Frankfurt"))
                .contains("Upstream Port");
    }

    @Test
    void hostnameZuKurz_meldetHostname() {
        assertThat(DeviceValidation.validate("sw", 5, "ab", "10.0.0.1", "Frankfurt"))
                .contains("Hostname");
    }

    @Test
    void hostnameZuLang_meldetHostname() {
        String tooLong = "a".repeat(31);
        assertThat(DeviceValidation.validate("sw", 5, tooLong, "10.0.0.1", "Frankfurt"))
                .contains("Hostname");
    }

    @Test
    void hostnameMitUnderscore_meldetHostname() {
        assertThat(DeviceValidation.validate("sw", 5, "sw_test", "10.0.0.1", "Frankfurt"))
                .contains("Hostname");
    }

    @Test
    void hostnameMitUmlaut_meldetHostname() {
        assertThat(DeviceValidation.validate("sw", 5, "sw-täst", "10.0.0.1", "Frankfurt"))
                .contains("Hostname");
    }

    @Test
    void ipDreiOctets_meldetIp() {
        assertThat(DeviceValidation.validate("sw", 5, "sw-test", "10.0.0", "Frankfurt"))
                .contains("IP");
    }

    @Test
    void ipOctet256_meldetIp() {
        assertThat(DeviceValidation.validate("sw", 5, "sw-test", "10.0.0.256", "Frankfurt"))
                .contains("IP");
    }

    @Test
    void ipMitBuchstabe_meldetIp() {
        assertThat(DeviceValidation.validate("sw", 5, "sw-test", "10.0.0.a", "Frankfurt"))
                .contains("IP");
    }

    @Test
    void leereLocation_meldetLocation() {
        assertThat(DeviceValidation.validate("sw", 5, "sw-test", "10.0.0.1", ""))
                .contains("Location");
    }

    @Test
    void minimalLaengeHostname3_istGueltig() {
        assertThat(DeviceValidation.validate("sw", 5, "abc", "10.0.0.1", "loc"))
                .isNull();
    }

    @Test
    void grenzfaellePort1Und48_sindGueltig() {
        assertThat(DeviceValidation.validate("sw", 1, "sw-test", "10.0.0.1", "loc")).isNull();
        assertThat(DeviceValidation.validate("sw", 48, "sw-test", "10.0.0.1", "loc")).isNull();
    }

    @Test
    void locationMitZeilenumbruch_meldetUngueltigeZeichen() {
        // newline could otherwise smuggle an additional command line into the cisco config
        assertThat(DeviceValidation.validate("sw", 5, "sw-test", "10.0.0.1", "Frankfurt\nno aaa"))
                .contains("invalid characters");
    }

    @Test
    void locationMitCarriageReturn_meldetUngueltigeZeichen() {
        assertThat(DeviceValidation.validate("sw", 5, "sw-test", "10.0.0.1", "Frankfurt\rRZ"))
                .contains("invalid characters");
    }

    @Test
    void locationZuLang_meldetUngueltigeZeichen() {
        String tooLong = "a".repeat(201);
        assertThat(DeviceValidation.validate("sw", 5, "sw-test", "10.0.0.1", tooLong))
                .contains("invalid characters");
    }

    @Test
    void locationMitTab_istGueltig() {
        // tab is allowed as a separator, only real control/line characters are blocked
        assertThat(DeviceValidation.validate("sw", 5, "sw-test", "10.0.0.1", "Frankfurt\tRZ-A"))
                .isNull();
    }

    @Test
    void upstreamSwitchMitZeilenumbruch_meldetUpstream() {
        // newline in the switch name could otherwise smuggle in an additional IOS line
        assertThat(DeviceValidation.validate("sw\nno aaa", 5, "sw-test", "10.0.0.1", "loc"))
                .contains("Upstream Switch");
    }

    @Test
    void upstreamSwitchZuLang_meldetUpstream() {
        assertThat(DeviceValidation.validate("a".repeat(51), 5, "sw-test", "10.0.0.1", "loc"))
                .contains("Upstream Switch");
    }

    @Test
    void upstreamSwitchMitPunktUndBindestrich_istGueltig() {
        assertThat(DeviceValidation.validate("sw-fra-01.lab", 5, "sw-test", "10.0.0.1", "loc"))
                .isNull();
    }

    @Test
    void isIpv4_gueltigeAdresse_true() {
        assertThat(DeviceValidation.isIpv4("192.168.1.1")).isTrue();
    }

    @Test
    void isIpv4_nullUndMuell_false() {
        assertThat(DeviceValidation.isIpv4(null)).isFalse();
        assertThat(DeviceValidation.isIpv4("")).isFalse();
        assertThat(DeviceValidation.isIpv4("10.0.0.256")).isFalse();
        assertThat(DeviceValidation.isIpv4("not-an-ip")).isFalse();
    }
}
