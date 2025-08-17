package ar.qubi.snmp.api;

import java.util.List;

public record SnmpWalkResult(List<Var> rows, long durationMs, String error) {
  public boolean ok(){ return error == null; }
}
