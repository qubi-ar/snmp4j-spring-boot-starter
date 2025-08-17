package ar.qubi.snmp.client;

import ar.qubi.snmp.api.*;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SnmpClientSnmp4j implements SnmpClient {
    private final Snmp snmp;
    private final TreeUtils treeUtils;

    public SnmpClientSnmp4j() throws IOException {
        TransportMapping<?> transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);
        
        // Add USM for SNMPv3 support
        USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);
        
        transport.listen();
        this.treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
    }

    @Override
    public SnmpResult get(TargetSpec target, String... oids) {
        long startTime = System.currentTimeMillis();
        try {
            Target snmpTarget = createTarget(target);
            PDU pdu = createPDU(target.version());
            
            for (String oid : oids) {
                pdu.add(new VariableBinding(new OID(oid)));
            }
            
            ResponseEvent response = snmp.send(pdu, snmpTarget);
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.getResponse() == null) {
                return new SnmpResult(List.of(), duration, "No response from target");
            }
            
            return processResponse(response.getResponse(), duration);
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            return new SnmpResult(List.of(), duration, "IO Error: " + e.getMessage());
        }
    }

    @Override
    public SnmpResult getNext(TargetSpec target, String... oids) {
        long startTime = System.currentTimeMillis();
        try {
            Target snmpTarget = createTarget(target);
            PDU pdu = createPDU(target.version());
            pdu.setType(PDU.GETNEXT);
            
            for (String oid : oids) {
                pdu.add(new VariableBinding(new OID(oid)));
            }
            
            ResponseEvent response = snmp.send(pdu, snmpTarget);
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.getResponse() == null) {
                return new SnmpResult(List.of(), duration, "No response from target");
            }
            
            return processResponse(response.getResponse(), duration);
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            return new SnmpResult(List.of(), duration, "IO Error: " + e.getMessage());
        }
    }

    @Override
    public SnmpResult bulk(TargetSpec target, int nonRepeaters, int maxRepetitions, String... oids) {
        long startTime = System.currentTimeMillis();
        try {
            Target snmpTarget = createTarget(target);
            PDU pdu = createPDU(target.version());
            
            if (pdu instanceof ScopedPDU) {
                pdu.setType(PDU.GETBULK);
                pdu.setNonRepeaters(nonRepeaters);
                pdu.setMaxRepetitions(maxRepetitions);
            } else {
                // Fall back to GETNEXT for SNMPv1
                pdu.setType(PDU.GETNEXT);
            }
            
            for (String oid : oids) {
                pdu.add(new VariableBinding(new OID(oid)));
            }
            
            ResponseEvent response = snmp.send(pdu, snmpTarget);
            long duration = System.currentTimeMillis() - startTime;
            
            if (response.getResponse() == null) {
                return new SnmpResult(List.of(), duration, "No response from target");
            }
            
            return processResponse(response.getResponse(), duration);
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            return new SnmpResult(List.of(), duration, "IO Error: " + e.getMessage());
        }
    }

    @Override
    public SnmpWalkResult walk(TargetSpec target, String rootOid) {
        long startTime = System.currentTimeMillis();
        try {
            Target snmpTarget = createTarget(target);
            List<TreeEvent> events = treeUtils.getSubtree(snmpTarget, new OID(rootOid));
            long duration = System.currentTimeMillis() - startTime;
            
            List<Var> vars = new ArrayList<>();
            String error = null;
            
            for (TreeEvent event : events) {
                if (event.isError()) {
                    error = "Walk error: " + event.getErrorMessage();
                    break;
                }
                
                VariableBinding[] vbs = event.getVariableBindings();
                if (vbs != null) {
                    for (VariableBinding vb : vbs) {
                        if (vb.getOid() != null && vb.getVariable() != null) {
                            vars.add(new Var(vb.getOid().toString(), vb.getVariable().toString()));
                        }
                    }
                }
            }
            
            return new SnmpWalkResult(vars, duration, error);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new SnmpWalkResult(List.of(), duration, "Walk error: " + e.getMessage());
        }
    }

    private Target createTarget(TargetSpec targetSpec) {
        String version = targetSpec.version() != null ? targetSpec.version() : "2c";
        String addressString = "udp:" + targetSpec.host() + "/" + targetSpec.port();
        Address address = GenericAddress.parse(addressString);
        
        if (address == null) {
            throw new IllegalArgumentException("Invalid host address: " + targetSpec.host());
        }
        
        switch (version) {
            case "1":
                CommunityTarget v1Target = new CommunityTarget();
                v1Target.setCommunity(new OctetString(targetSpec.community() != null ? targetSpec.community() : "public"));
                v1Target.setAddress(address);
                v1Target.setVersion(SnmpConstants.version1);
                v1Target.setTimeout(5000);
                v1Target.setRetries(2);
                return v1Target;
                
            case "2c":
                CommunityTarget v2Target = new CommunityTarget();
                v2Target.setCommunity(new OctetString(targetSpec.community() != null ? targetSpec.community() : "public"));
                v2Target.setAddress(address);
                v2Target.setVersion(SnmpConstants.version2c);
                v2Target.setTimeout(5000);
                v2Target.setRetries(2);
                return v2Target;
                
            case "3":
                UserTarget v3Target = new UserTarget();
                v3Target.setAddress(address);
                v3Target.setVersion(SnmpConstants.version3);
                v3Target.setTimeout(5000);
                v3Target.setRetries(2);
                
                if (targetSpec.v3() != null) {
                    v3Target.setSecurityLevel(parseSecurityLevel(targetSpec.v3().securityLevel()));
                    v3Target.setSecurityName(new OctetString(targetSpec.v3().username()));
                    
                    // Add user to USM if not already present
                    addUser(targetSpec.v3());
                }
                return v3Target;
                
            default:
                throw new IllegalArgumentException("Unsupported SNMP version: " + version);
        }
    }

    private void addUser(V3Sec v3Sec) {
        USM usm = (USM) SecurityModels.getInstance().getSecurityModel(new Integer32(MPv3.ID));
        
        OID authProtocol = null;
        OID privProtocol = null;
        
        if (v3Sec.authPass() != null && !v3Sec.authPass().isEmpty()) {
            authProtocol = v3Sec.authProtocol() != null ? 
                new OID(v3Sec.authProtocol()) : AuthSHA.ID;
        }
        
        if (v3Sec.privPass() != null && !v3Sec.privPass().isEmpty()) {
            privProtocol = v3Sec.privProtocol() != null ? 
                new OID(v3Sec.privProtocol()) : PrivAES128.ID;
        }
        
        UsmUser user = new UsmUser(
            new OctetString(v3Sec.username()),
            authProtocol,
            v3Sec.authPass() != null ? new OctetString(v3Sec.authPass()) : null,
            privProtocol,
            v3Sec.privPass() != null ? new OctetString(v3Sec.privPass()) : null
        );
        
        usm.addUser(user);
    }
    
    private int parseSecurityLevel(String securityLevel) {
        if (securityLevel == null) {
            return SecurityLevel.NOAUTH_NOPRIV;
        }
        
        switch (securityLevel.toLowerCase()) {
            case "noauth":
            case "noauthnopriv":
                return SecurityLevel.NOAUTH_NOPRIV;
            case "auth":
            case "authnopriv":
                return SecurityLevel.AUTH_NOPRIV;
            case "authpriv":
                return SecurityLevel.AUTH_PRIV;
            default:
                return SecurityLevel.NOAUTH_NOPRIV;
        }
    }

    private PDU createPDU(String version) {
        if ("3".equals(version)) {
            return new ScopedPDU();
        } else {
            return new PDU();
        }
    }

    private SnmpResult processResponse(PDU response, long duration) {
        if (response.getErrorStatus() != 0) {
            return new SnmpResult(List.of(), duration, "SNMP Error: " + response.getErrorStatusText());
        }
        
        List<Var> vars = new ArrayList<>();
        for (VariableBinding vb : response.getVariableBindings()) {
            if (vb.getOid() != null && vb.getVariable() != null) {
                vars.add(new Var(vb.getOid().toString(), vb.getVariable().toString()));
            }
        }
        
        return new SnmpResult(vars, duration, null);
    }

    public void close() throws IOException {
        if (snmp != null) {
            snmp.close();
        }
    }
}