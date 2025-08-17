package ar.qubi.snmp.client;

import ar.qubi.snmp.api.*;

public interface SnmpClient {
  SnmpResult get(TargetSpec target, String... oids);
  SnmpResult getNext(TargetSpec target, String... oids);
  SnmpResult bulk(TargetSpec target, int nonRepeaters, int maxRepetitions, String... oids);
  SnmpWalkResult walk(TargetSpec target, String rootOid);
}
