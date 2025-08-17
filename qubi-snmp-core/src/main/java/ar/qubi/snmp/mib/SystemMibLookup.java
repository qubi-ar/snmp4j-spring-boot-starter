package ar.qubi.snmp.mib;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Standard system MIB lookup with common OIDs.
 * Provides basic system and standard MIB entries.
 */
public class SystemMibLookup implements MibLookup {
    
    private static final Map<String, MibNode> MIB_ENTRIES = new HashMap<>();
    
    static {
        // System Group (RFC 1213)
        MIB_ENTRIES.put("1.3.6.1.2.1.1.1.0", new MibNode("1.3.6.1.2.1.1.1.0", "sysDescr", "System description", "DisplayString"));
        MIB_ENTRIES.put("1.3.6.1.2.1.1.2.0", new MibNode("1.3.6.1.2.1.1.2.0", "sysObjectID", "System object identifier", "ObjectIdentifier"));
        MIB_ENTRIES.put("1.3.6.1.2.1.1.3.0", new MibNode("1.3.6.1.2.1.1.3.0", "sysUpTime", "System uptime", "TimeTicks"));
        MIB_ENTRIES.put("1.3.6.1.2.1.1.4.0", new MibNode("1.3.6.1.2.1.1.4.0", "sysContact", "System contact", "DisplayString"));
        MIB_ENTRIES.put("1.3.6.1.2.1.1.5.0", new MibNode("1.3.6.1.2.1.1.5.0", "sysName", "System name", "DisplayString"));
        MIB_ENTRIES.put("1.3.6.1.2.1.1.6.0", new MibNode("1.3.6.1.2.1.1.6.0", "sysLocation", "System location", "DisplayString"));
        MIB_ENTRIES.put("1.3.6.1.2.1.1.7.0", new MibNode("1.3.6.1.2.1.1.7.0", "sysServices", "System services", "Integer"));
        
        // Interface Group (RFC 1213)
        MIB_ENTRIES.put("1.3.6.1.2.1.2.1.0", new MibNode("1.3.6.1.2.1.2.1.0", "ifNumber", "Number of network interfaces", "Integer"));
        MIB_ENTRIES.put("1.3.6.1.2.1.2.2.1.1", new MibNode("1.3.6.1.2.1.2.2.1.1", "ifIndex", "Interface index", "Integer"));
        MIB_ENTRIES.put("1.3.6.1.2.1.2.2.1.2", new MibNode("1.3.6.1.2.1.2.2.1.2", "ifDescr", "Interface description", "DisplayString"));
        MIB_ENTRIES.put("1.3.6.1.2.1.2.2.1.3", new MibNode("1.3.6.1.2.1.2.2.1.3", "ifType", "Interface type", "Integer"));
        MIB_ENTRIES.put("1.3.6.1.2.1.2.2.1.4", new MibNode("1.3.6.1.2.1.2.2.1.4", "ifMtu", "Interface MTU", "Integer"));
        MIB_ENTRIES.put("1.3.6.1.2.1.2.2.1.5", new MibNode("1.3.6.1.2.1.2.2.1.5", "ifSpeed", "Interface speed", "Gauge"));
        MIB_ENTRIES.put("1.3.6.1.2.1.2.2.1.6", new MibNode("1.3.6.1.2.1.2.2.1.6", "ifPhysAddress", "Interface physical address", "PhysAddress"));
        MIB_ENTRIES.put("1.3.6.1.2.1.2.2.1.7", new MibNode("1.3.6.1.2.1.2.2.1.7", "ifAdminStatus", "Interface admin status", "Integer"));
        MIB_ENTRIES.put("1.3.6.1.2.1.2.2.1.8", new MibNode("1.3.6.1.2.1.2.2.1.8", "ifOperStatus", "Interface operational status", "Integer"));
        
        // IP Group (RFC 1213)
        MIB_ENTRIES.put("1.3.6.1.2.1.4.1.0", new MibNode("1.3.6.1.2.1.4.1.0", "ipForwarding", "IP forwarding enabled", "Integer"));
        MIB_ENTRIES.put("1.3.6.1.2.1.4.2.0", new MibNode("1.3.6.1.2.1.4.2.0", "ipDefaultTTL", "Default IP TTL", "Integer"));
        
        // SNMP Group (RFC 1213)
        MIB_ENTRIES.put("1.3.6.1.2.1.11.1.0", new MibNode("1.3.6.1.2.1.11.1.0", "snmpInPkts", "Total SNMP packets received", "Counter"));
        MIB_ENTRIES.put("1.3.6.1.2.1.11.3.0", new MibNode("1.3.6.1.2.1.11.3.0", "snmpInBadVersions", "SNMP packets with bad version", "Counter"));
        MIB_ENTRIES.put("1.3.6.1.2.1.11.4.0", new MibNode("1.3.6.1.2.1.11.4.0", "snmpInBadCommunityNames", "SNMP packets with bad community", "Counter"));
        MIB_ENTRIES.put("1.3.6.1.2.1.11.5.0", new MibNode("1.3.6.1.2.1.11.5.0", "snmpInBadCommunityUses", "SNMP packets with bad community use", "Counter"));
        
        // Common Trap OIDs (RFC 1215)
        MIB_ENTRIES.put("1.3.6.1.6.3.1.1.5.1", new MibNode("1.3.6.1.6.3.1.1.5.1", "coldStart", "Cold start trap", "NOTIFICATION-TYPE"));
        MIB_ENTRIES.put("1.3.6.1.6.3.1.1.5.2", new MibNode("1.3.6.1.6.3.1.1.5.2", "warmStart", "Warm start trap", "NOTIFICATION-TYPE"));
        MIB_ENTRIES.put("1.3.6.1.6.3.1.1.5.3", new MibNode("1.3.6.1.6.3.1.1.5.3", "linkDown", "Link down trap", "NOTIFICATION-TYPE"));
        MIB_ENTRIES.put("1.3.6.1.6.3.1.1.5.4", new MibNode("1.3.6.1.6.3.1.1.5.4", "linkUp", "Link up trap", "NOTIFICATION-TYPE"));
        MIB_ENTRIES.put("1.3.6.1.6.3.1.1.5.5", new MibNode("1.3.6.1.6.3.1.1.5.5", "authenticationFailure", "Authentication failure trap", "NOTIFICATION-TYPE"));
        MIB_ENTRIES.put("1.3.6.1.6.3.1.1.5.6", new MibNode("1.3.6.1.6.3.1.1.5.6", "egpNeighborLoss", "EGP neighbor loss trap", "NOTIFICATION-TYPE"));
        
        // SNMPv2 Trap OIDs
        MIB_ENTRIES.put("1.3.6.1.6.3.1.1.4.1.0", new MibNode("1.3.6.1.6.3.1.1.4.1.0", "snmpTrapOID", "The trap OID", "ObjectIdentifier"));
        MIB_ENTRIES.put("1.3.6.1.2.1.1.3.0", new MibNode("1.3.6.1.2.1.1.3.0", "sysUpTimeInstance", "System uptime instance", "TimeTicks"));
        
        // Host Resources MIB (RFC 2790) - Common entries
        MIB_ENTRIES.put("1.3.6.1.2.1.25.1.1.0", new MibNode("1.3.6.1.2.1.25.1.1.0", "hrSystemUptime", "Host system uptime", "TimeTicks"));
        MIB_ENTRIES.put("1.3.6.1.2.1.25.1.2.0", new MibNode("1.3.6.1.2.1.25.1.2.0", "hrSystemDate", "Host system date", "DateAndTime"));
        MIB_ENTRIES.put("1.3.6.1.2.1.25.1.3.0", new MibNode("1.3.6.1.2.1.25.1.3.0", "hrSystemInitialLoadDevice", "System boot device", "Integer"));
        MIB_ENTRIES.put("1.3.6.1.2.1.25.1.4.0", new MibNode("1.3.6.1.2.1.25.1.4.0", "hrSystemInitialLoadParameters", "System boot parameters", "InternationalDisplayString"));
        MIB_ENTRIES.put("1.3.6.1.2.1.25.1.5.0", new MibNode("1.3.6.1.2.1.25.1.5.0", "hrSystemNumUsers", "Number of logged users", "Gauge"));
        MIB_ENTRIES.put("1.3.6.1.2.1.25.1.6.0", new MibNode("1.3.6.1.2.1.25.1.6.0", "hrSystemProcesses", "Number of running processes", "Gauge"));
        MIB_ENTRIES.put("1.3.6.1.2.1.25.1.7.0", new MibNode("1.3.6.1.2.1.25.1.7.0", "hrSystemMaxProcesses", "Maximum number of processes", "Integer"));
    }

    @Override
    public Optional<MibNode> find(String oid) {
        if (oid == null) {
            return Optional.empty();
        }
        
        // Try exact match first
        MibNode node = MIB_ENTRIES.get(oid);
        if (node != null) {
            return Optional.of(node);
        }
        
        // Try to find parent OID for table entries (e.g., 1.3.6.1.2.1.2.2.1.1.1 -> 1.3.6.1.2.1.2.2.1.1)
        String[] parts = oid.split("\\.");
        if (parts.length > 4) {
            // Try removing the last part (instance index)
            String parentOid = String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1));
            node = MIB_ENTRIES.get(parentOid);
            if (node != null) {
                return Optional.of(new MibNode(oid, node.name() + "." + parts[parts.length - 1], node.description(), node.syntax()));
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Get all known MIB entries
     */
    public Map<String, MibNode> getAllEntries() {
        return new HashMap<>(MIB_ENTRIES);
    }
}
