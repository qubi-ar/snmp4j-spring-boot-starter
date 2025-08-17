package ar.qubi.snmp.mib;

import java.util.Optional;

public interface MibLookup {
  Optional<MibNode> find(String oid);
}
