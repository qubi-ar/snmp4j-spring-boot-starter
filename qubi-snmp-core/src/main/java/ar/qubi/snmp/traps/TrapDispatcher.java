package ar.qubi.snmp.traps;

import ar.qubi.snmp.api.TrapMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class TrapDispatcher implements Consumer<TrapMessage> {
  private final List<Consumer<TrapMessage>> consumers = new CopyOnWriteArrayList<>();
  public void add(Consumer<TrapMessage> c) { if (c != null) consumers.add(c); }
  public void remove(Consumer<TrapMessage> c) { consumers.remove(c); }
  @Override public void accept(TrapMessage msg) {
    for (var c : consumers) {
      try { c.accept(msg); } catch (Exception e) {
        System.err.println("Error in trap listener: " + e.getMessage());
      }
    }
  }
  public int size() { return consumers.size(); }
}
