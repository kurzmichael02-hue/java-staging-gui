# Staging GUI

Web tool for staging Cisco Catalyst 9000 switches before customer deployment.
You create a project, enter the network data once, and capture the devices via
form or Excel import. The tool builds the IOS config from that and sends the
provisioning data to the ZTP server.

## Background

Built as my practical project during the dual study program at Logicalis
(network department). The as-is analysis of the existing staging process found
two main weaknesses: every switch was configured fully by hand (data entry,
duplicate check, status monitoring across several systems, hunting for typos,
spotting port conflicts), and the Excel/VBA helper that was supposed to speed
this up had been blocked by the Windows security policy. This tool replaces
both: network data is entered once per project, the IOS config is generated per
profile, and the provisioning data goes to the ZTP server with one click.

The thesis estimates the per-device effort dropping from about 25 to 4 minutes
(roughly 21 minutes saved). Most of that comes from the follow-up work, not the
data entry itself: hunting down typos (9 min) and watching the status across
systems (5 min) make up two thirds of the saving. These are estimates; a
measured run on real switches was still outstanding.

## Setup

Requires JDK 17. The Maven wrapper is in the repo.

```
./mvnw spring-boot:run        # dev, http://localhost:8080
./mvnw test                   # tests
./mvnw clean package          # builds the jar into target/
```

On first start the H2 database is created as a file (`staging-gui-db.mv.db`)
next to the process. Tables are created automatically via Hibernate
(`ddl-auto=update`).

## Config

Important values in `src/main/resources/application.properties`, overridable in
prod via env var:

- `ztp.server.baseurl` - URL of the ZTP server
- `ADMIN_PASSWORD` - Cisco admin secret, required (app won't start otherwise)
- `SNMP_COMMUNITY` - SNMP RO community, required
- `app.stagers` - comma-separated list of stagers for the dropdown
- `app.manual-baseline-minutes-per-device` - comparison value for the metrics
- `DB_FILE` - path of the H2 file

## Structure

```
  input                     core                        output
  ─────                     ────                        ──────
  4-step wizard  ───────►  Project + Device  ──────►  ConfigGenerator
  (project, hw,            (H2 via JPA)                (IOS config S/M/L)
   network, devices)             │                           │
  Excel import  ────────────────►│                           ▼
                                 │                        ZtpClient
                                 ▼                     (JSON ─► ZTP server)
                          /devices dashboard  ◄── status ─────┘
                          /metrics · /audit · PDF report
```

Classic Spring MVC, Controller -> Service -> JPA repository.

- **Wizard** (4 steps): Project -> Hardware config -> Network defaults -> Devices.
  Project and devices live in the DB, the session only holds the project title.
- **ConfigGenerator** builds the IOS config depending on profile S/M/L (Lite /
  Standard / Extended).
- **ZtpClient** sends the provisioning data as JSON to the ZTP server.
- **Dashboard** (`/devices`): device table, status filter, bulk start,
  Excel import, CSV export.
- **Metrics** (`/metrics`): time comparison manual vs. tool, stager breakdown.
- **Audit log** (`/audit`): who did what and when.
- **Health** (`/actuator/health`): for PRTG monitoring.

Profiles:

| Profile | Contents |
|--------|--------|
| S (Lite) | Hostname, Management IP, Gateway, DNS, SSH |
| M (Standard) | S + uplink port (trunk), Spanning-Tree Portfast |
| L (Extended) | M + AAA, NTP, DHCP snooping, SNMP, Syslog, Banner |

## Running as a service

Build the jar and start it with the two required env vars set:

```
./mvnw clean package
ADMIN_PASSWORD=... SNMP_COMMUNITY=... java -jar target/java-staging-gui-0.0.1-SNAPSHOT.jar
```

Internally it ran as a systemd service; any host with JDK 17 and a reachable
ZTP server works.

## Stack

Spring Boot 4, Java 17, Thymeleaf, Spring Data JPA + H2, Apache POI (Excel),
Bootstrap 5.
