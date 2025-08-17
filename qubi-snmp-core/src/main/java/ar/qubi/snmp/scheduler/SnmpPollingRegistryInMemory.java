package ar.qubi.snmp.scheduler;

import ar.qubi.snmp.client.SnmpClient;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class SnmpPollingRegistryInMemory implements SnmpPollingRegistry {
  private final ScheduledExecutorService exec;
  private final SnmpClient client;
  private final Map<String,ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
  private final Logger log = Logger.getLogger(getClass().getName());

  public SnmpPollingRegistryInMemory(ScheduledExecutorService exec, SnmpClient client) {
    this.exec = exec; this.client = client;
  }

  @Override public String register(PollingJob job, JobConfig cfg) {
    String id = job.name() + "-" + UUID.randomUUID();
    long periodMs = (cfg.fixedRate() == null ? java.time.Duration.ofSeconds(30) : cfg.fixedRate()).toMillis();
    ScheduledFuture<?> f = exec.scheduleAtFixedRate(() -> {
      try {
        job.execute(new JobContext() {
          @Override public java.util.Map<String, Object> params() { return cfg.params(); }
          @Override public SnmpClient client() { return client; }
          @Override public Logger logger() { return log; }
          @Override public void recordMetric(String key, double value) { /* no-op stub */ }
        });
      } catch (Exception ex) {
        log.warning("Job failed: " + job.name() + " at " + Instant.now() + " - " + ex.getMessage());
      }
    }, 0, periodMs, TimeUnit.MILLISECONDS);
    tasks.put(id, f);
    return id;
  }

  @Override public boolean cancel(String jobId) {
    var f = tasks.remove(jobId);
    return f != null && f.cancel(true);
  }
}
