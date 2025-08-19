package ar.qubi.snmp.scheduler;

import ar.qubi.snmp.client.SnmpClient;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
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
    
    Runnable task = () -> {
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
    };
    
    ScheduledFuture<?> f;
    
    // Use cron expression if provided, otherwise fall back to fixed rate
    if (cfg.cron() != null && !cfg.cron().trim().isEmpty()) {
      f = scheduleCronJob(task, cfg.cron());
    } else {
      long periodMs = (cfg.fixedRate() == null ? java.time.Duration.ofSeconds(30) : cfg.fixedRate()).toMillis();
      f = exec.scheduleAtFixedRate(task, 0, periodMs, TimeUnit.MILLISECONDS);
    }
    
    tasks.put(id, f);
    return id;
  }
  
  private ScheduledFuture<?> scheduleCronJob(Runnable task, String cronExpression) {
    return exec.schedule(new Runnable() {
      @Override
      public void run() {
        try {
          task.run();
        } finally {
          // Schedule next execution based on cron expression
          long nextDelayMs = getNextDelayFromCron(cronExpression);
          if (nextDelayMs > 0) {
            exec.schedule(this, nextDelayMs, TimeUnit.MILLISECONDS);
          }
        }
      }
    }, getNextDelayFromCron(cronExpression), TimeUnit.MILLISECONDS);
  }
  
  private long getNextDelayFromCron(String cronExpression) {
    try {
      return CronParser.getNextDelayMillis(cronExpression);
    } catch (Exception e) {
      log.warning("Invalid cron expression '" + cronExpression + "': " + e.getMessage() + 
                 ". Falling back to 30 second interval.");
      return 30000; // 30 seconds fallback
    }
  }

  @Override public boolean cancel(String jobId) {
    var f = tasks.remove(jobId);
    return f != null && f.cancel(true);
  }
}
