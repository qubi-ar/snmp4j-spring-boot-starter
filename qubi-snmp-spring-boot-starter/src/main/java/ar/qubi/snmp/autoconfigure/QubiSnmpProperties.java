package ar.qubi.snmp.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qubi.snmp")
public class QubiSnmpProperties {
  private boolean enabled = true;
  private Traps traps = new Traps();
  private Client client = new Client();
  private Mib mib = new Mib();
  private Scheduler scheduler = new Scheduler();

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public Traps getTraps() { return traps; }
  public void setTraps(Traps traps) { this.traps = traps; }

  public Client getClient() { return client; }
  public void setClient(Client client) { this.client = client; }

  public Mib getMib() { return mib; }
  public void setMib(Mib mib) { this.mib = mib; }

  public Scheduler getScheduler() { return scheduler; }
  public void setScheduler(Scheduler scheduler) { this.scheduler = scheduler; }

  public static class Traps {
    private boolean enabled = true;
    private int port = 9162;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
  }

  public static class Client {
    private int timeoutMs = 2000;
    private int retries = 1;
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getRetries() { return retries; }
    public void setRetries(int retries) { this.retries = retries; }
  }

  public static class Mib {
    private String lookup = "auto";
    public String getLookup() { return lookup; }
    public void setLookup(String lookup) { this.lookup = lookup; }
  }

  public static class Scheduler {
    private boolean enabled = true;
    private Pool pool = new Pool();
    private Log log = new Log();
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Pool getPool() { return pool; }
    public void setPool(Pool pool) { this.pool = pool; }
    public Log getLog() { return log; }
    public void setLog(Log log) { this.log = log; }

    public static class Pool {
      private int coreSize = 4;
      public int getCoreSize() { return coreSize; }
      public void setCoreSize(int coreSize) { this.coreSize = coreSize; }
    }

    public static class Log {
      private int capacity = 1000;
      public int getCapacity() { return capacity; }
      public void setCapacity(int capacity) { this.capacity = capacity; }
    }
  }
}
