package ar.qubi.snmp.scheduler;
public interface PollingJob {
  String name();
  void execute(JobContext ctx) throws Exception;
}
