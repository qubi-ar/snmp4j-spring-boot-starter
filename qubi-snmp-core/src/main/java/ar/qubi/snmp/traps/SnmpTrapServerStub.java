package ar.qubi.snmp.traps;

import java.util.concurrent.atomic.AtomicBoolean;

public class SnmpTrapServerStub {
  private final AtomicBoolean running = new AtomicBoolean(false);
  public void start() { running.set(true); }
  public void stop() { running.set(false); }
  public boolean isRunning() { return running.get(); }
}
