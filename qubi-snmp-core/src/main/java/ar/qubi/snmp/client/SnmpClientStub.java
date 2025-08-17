package ar.qubi.snmp.client;

import ar.qubi.snmp.api.*;
import java.util.List;

public class SnmpClientStub implements SnmpClient {
  @Override public SnmpResult get(TargetSpec t, String... oids){ return new SnmpResult(List.of(), 0, null); }
  @Override public SnmpResult getNext(TargetSpec t, String... oids){ return new SnmpResult(List.of(), 0, null); }
  @Override public SnmpResult bulk(TargetSpec t, int n, int m, String... oids){ return new SnmpResult(List.of(), 0, null); }
  @Override public SnmpWalkResult walk(TargetSpec t, String root){ return new SnmpWalkResult(List.of(), 0, null); }
}
