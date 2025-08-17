package ar.qubi.snmp.scheduler;

import ar.qubi.snmp.api.TargetSpec;
import java.time.Duration;
import java.util.Map;

public record JobConfig(String cron, Duration fixedRate, TargetSpec target, Map<String,Object> params) {}
