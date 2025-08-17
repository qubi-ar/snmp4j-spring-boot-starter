package ar.qubi.snmp.scheduler;

public interface SnmpPollingRegistry {
  String register(PollingJob job, JobConfig config);
  boolean cancel(String jobId);
}
