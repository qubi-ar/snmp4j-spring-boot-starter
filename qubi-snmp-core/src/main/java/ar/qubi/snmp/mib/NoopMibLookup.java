package ar.qubi.snmp.mib;

import java.util.Optional;

public class NoopMibLookup implements MibLookup {
  @Override public Optional<MibNode> find(String oid) { return Optional.of(new MibNode(oid, oid, "N/A", "N/A")); }
}
