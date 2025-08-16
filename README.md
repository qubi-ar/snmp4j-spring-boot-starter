# Qubi SNMP Spring Boot Starter

**GroupId:** `ar.qubi`
**ArtifactId:** `qubi-snmp-spring-boot-starter`
**Java:** 17+
**Spring Boot:** 3.2+

------------------------------------------------------------------------

## Overview

Qubi SNMP Starter is a **Spring Boot auto-configuration library** that
brings SNMP monitoring capabilities into any application with minimal
setup.

Features: - **Trap receiver (v1/v2c/v3)** with annotation-based
listeners.
- **Polling jobs** (fixed rate or cron) executed by a configurable
  `ExecutorService` (no Quartz).
- **On-demand SNMP client** for GET, GETNEXT, BULK, and WALK.
- **MIB lookup** using system MIBs (via `snmptranslate`) with fallback
  when unavailable.
- **In-memory metrics** (running/scheduled/completed/failed) and
  **execution log** for jobs.
- **Spring Boot auto-config** with `@ConfigurationProperties` and
  optional Actuator endpoint.

------------------------------------------------------------------------

## Getting Started

### 1. Add dependency

``` xml
<dependency>
  <groupId>ar.qubi</groupId>
  <artifactId>qubi-snmp-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### 2. Enable traps and scheduler

``` yaml
qubi:
  snmp:
    traps:
      enabled: true
      port: 9162
      v2c:
        communities: [ "public" ]
    scheduler:
      enabled: true
      pool:
        core-size: 4
```

### 3. Listen to traps

``` java
@Component
class TrapHandlers {

  @SnmpTrapListener(oids = {"1.3.6.1.6.3.1.1.5.3", "1.3.6.1.6.3.1.1.5.4"}) // linkDown, linkUp
  public void onIfStatus(TrapMessage msg) {
    msg.variables().forEach(v -> {
      msg.mib().get().find(v.oid()).ifPresentOrElse(
        node -> System.out.printf("%s = %s (%s)%n", node.name(), v.valueAsString(), node.syntax()),
        () -> System.out.printf("%s = %s%n", v.oid(), v.valueAsString())
      );
    });
  }
}
```

### 4. Schedule a polling job

``` java
@Autowired SnmpPollingRegistry registry;

@PostConstruct
void setup() {
  var job = new IfTableJob();
  var cfg = new JobConfig(null, Duration.ofSeconds(30),
      new TargetSpec("10.0.0.5", 161, "v2c", "public", null),
      Map.of("rootOid", "1.3.6.1.2.1.2.2"));
  registry.register(job, cfg);
}
```

------------------------------------------------------------------------

## Configuration Properties

``` yaml
qubi:
  snmp:
    traps:
      enabled: true
      port: 9162
    client:
      timeout-ms: 2000
      retries: 1
    mib:
      lookup: auto          # auto | netsnmp | noop
    scheduler:
      enabled: true
      pool:
        core-size: 4
      log:
        capacity: 1000
```

------------------------------------------------------------------------

## Quick Demo Application

Create a simple Spring Boot app:

``` java
@SpringBootApplication
public class SnmpDemoApp {
  public static void main(String[] args) {
    SpringApplication.run(SnmpDemoApp.class, args);
  }
}

@Component
class TrapLogger {
  @SnmpTrapListener
  public void logTrap(TrapMessage msg) {
    System.out.println("Trap received: " + msg);
  }
}

@Component
class SysDescrJob implements PollingJob {
  public String name() { return "sysDescr"; }
  public void execute(JobContext ctx) {
    var res = ctx.client().get(
      new TargetSpec("127.0.0.1", 161, "v2c", "public", null),
      "1.3.6.1.2.1.1.1.0"
    );
    ctx.logger().info("sysDescr={}", res.asMap());
  }
}
```

Run with:

``` bash
mvn spring-boot:run
```

------------------------------------------------------------------------

## Metrics & Observability

-   Exposes gauges and counters via Micrometer:
    -   `qubi.snmp.scheduler.running`
    -   `scheduled.total`, `completed.total`, `failed.total`
    -   `job.duration` (timer, tagged by job & target)
-   Optional Actuator endpoint:
    -   `/actuator/qubiSnmp/metrics`\
    -   `/actuator/qubiSnmp/jobs/recent`

------------------------------------------------------------------------

## License & MIBs

This project **does not bundle MIBs**. If Net-SNMP and its MIBs are
installed on the system, the starter uses them via `snmptranslate`.
Otherwise, a fallback `NoopMibLookup` is used (OIDs only).

------------------------------------------------------------------------

## Roadmap

-   Retry with exponential backoff.
-   Rate-limit per host.
-   Optional persistence for execution logs.
-   TLS/DTLS support for transport.
-   Health indicators for SNMP endpoints.

------------------------------------------------------------------------

## Status

🚧 **Early stage (v0.1.0)** --- functional traps, client, scheduler,
metrics.
Expect API adjustments until a stable release.# qubi-snmp-spring-boot-starter
# snmp4j-spring-boot-starter
# snmp4j-spring-boot-starter
