package ar.qubi.snmp.traps;

import ar.qubi.snmp.api.TrapMessage;
import java.util.List;
import java.util.function.Consumer;

public class SnmpTrapDispatcherStub {
  private final List<Consumer<TrapMessage>> listeners;
  public SnmpTrapDispatcherStub(List<Consumer<TrapMessage>> listeners) { this.listeners = listeners; }
  public void dispatch(TrapMessage msg) { listeners.forEach(l -> { try { l.accept(msg); } catch (Exception ignored) {} }); }
}
