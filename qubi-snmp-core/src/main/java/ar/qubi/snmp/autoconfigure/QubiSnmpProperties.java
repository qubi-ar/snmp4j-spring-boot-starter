package ar.qubi.snmp.autoconfigure;


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
    private int version = 2;
    private Security security = new Security();
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public static class Security {
      private String communityString = "public";
      private String securityName;
      private String authenticationProtocol;
      private String authenticationPassphrase;
      private String privacyProtocol;
      private String privacyPassphrase;
      private String contextName;
      private String contextEngineId;

      public String getCommunityString() { return communityString; }
      public void setCommunityString(String communityString) { this.communityString = communityString; }
      public String getSecurityName() { return securityName; }
      public void setSecurityName(String securityName) { this.securityName = securityName; }
      public String getAuthenticationProtocol() { return authenticationProtocol; }
      public void setAuthenticationProtocol(String authenticationProtocol) { this.authenticationProtocol = authenticationProtocol; }
      public String getAuthenticationPassphrase() { return authenticationPassphrase; }
      public void setAuthenticationPassphrase(String authenticationPassphrase) { this.authenticationPassphrase = authenticationPassphrase; }
      public String getPrivacyProtocol() { return privacyProtocol; }
      public void setPrivacyProtocol(String privacyProtocol) { this.privacyProtocol = privacyProtocol; }
      public String getPrivacyPassphrase() { return privacyPassphrase; }
      public void setPrivacyPassphrase(String privacyPassphrase) { this.privacyPassphrase = privacyPassphrase; }
      public String getContextName() { return contextName; }
      public void setContextName(String contextName) { this.contextName = contextName; }
      public String getContextEngineId() { return contextEngineId; }
      public void setContextEngineId(String contextEngineId) { this.contextEngineId = contextEngineId; }
    }
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
    private String cron;
    private int fixedRateSeconds = 10;
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Pool getPool() { return pool; }
    public void setPool(Pool pool) { this.pool = pool; }
    public Log getLog() { return log; }
    public void setLog(Log log) { this.log = log; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public int getFixedRateSeconds() { return fixedRateSeconds; }
    public void setFixedRateSeconds(int fixedRateSeconds) { this.fixedRateSeconds = fixedRateSeconds; }

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
