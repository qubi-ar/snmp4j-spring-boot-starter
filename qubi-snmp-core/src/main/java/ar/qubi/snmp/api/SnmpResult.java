package ar.qubi.snmp.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record SnmpResult(List<Var> vars, long durationMs, String error) {
  public boolean ok() { return error == null; }
  public Map<String,String> asMap() {
    return vars == null ? Map.of() : vars.stream().collect(Collectors.toMap(Var::oid, Var::valueAsString, (a,b)->b));
  }
}
