package ar.qubi.snmp.api;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import ar.qubi.snmp.mib.MibLookup;

public record TrapMessage(
    Instant receivedAt,
    String remoteAddress,
    String version,
    String communityOrUser,
    String trapOid,
    List<Var> variables,
    Supplier<MibLookup> mib
) {}
