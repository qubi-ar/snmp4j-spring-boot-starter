package ar.qubi.snmp.traps;

import ar.qubi.snmp.api.TrapMessage;
import ar.qubi.snmp.api.Var;
import ar.qubi.snmp.autoconfigure.QubiSnmpProperties;
import ar.qubi.snmp.mib.MibLookup;
import org.snmp4j.*;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.*;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SnmpTrapServerSnmp4j implements CommandResponder {
  private final int port;
  private final Consumer<TrapMessage> dispatcher;
  private final Supplier<MibLookup> mib;
  private DefaultUdpTransportMapping transport;
  private Snmp snmp;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private QubiSnmpProperties.Traps.Security v3Security;
  public void setV3Security(QubiSnmpProperties.Traps.Security v3Security) {
    this.v3Security = v3Security;
  }

  public SnmpTrapServerSnmp4j(QubiSnmpProperties.Traps.Security props,int port, Consumer<TrapMessage> dispatcher, Supplier<MibLookup> mib) {
    this.port = port;
    this.dispatcher = dispatcher;
    this.mib = mib;
    this.v3Security = props;
  }


  // Optional: setter for cases where constructor injection is not used


  public synchronized void start() {
    if (running.get()) return;
    try {
      // Bind explicitly to IPv4 ANY to avoid accidental IPv6-only binds
      transport = new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/" + port));
      snmp = new Snmp(transport);

      // Register message processing models for v1, v2c, and v3
      snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
      snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
      snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3());

      // USM setup for v3 notifications
      SecurityProtocols.getInstance().addDefaultProtocols();
      USM usm = new USM(
              SecurityProtocols.getInstance(),
              new OctetString(MPv3.createLocalEngineID()),
              0
      );
      // Enable engine discovery for trap reception
      usm.setEngineDiscoveryEnabled(true);
      SecurityModels.getInstance().addSecurityModel(usm);

      // Configure USM user from properties if provided
      if (v3Security != null) {
        String user = trimToNull(v3Security.getSecurityName());
        String authProtoStr = trimToNull(v3Security.getAuthenticationProtocol());
        String authPass = trimToNull(v3Security.getAuthenticationPassphrase());
        String privProtoStr = trimToNull(v3Security.getPrivacyProtocol());
        String privPass = trimToNull(v3Security.getPrivacyPassphrase());

        // Determine protocols
        OID authProtocol = toAuthOID(authProtoStr);
        OID privProtocol = toPrivOID(privProtoStr);

        // Enforce SNMPv3 requirements: privacy implies authentication
        if (authProtocol == null) {
          authPass = null; // ignore auth pass if protocol is not set
          if (privProtocol != null) {
            // Privacy cannot be enabled without authentication
            privProtocol = null;
            privPass = null;
            System.out.println("[SNMP] v3 privacy protocol ignored because authentication protocol is not set");
          }
        }

        // Only add a user if a username is present
        if (user != null) {
          UsmUser usmUser = new UsmUser(
                  new OctetString(user),
                  authProtocol,
                  authPass != null ? new OctetString(authPass) : null,
                  privProtocol,
                  privPass != null ? new OctetString(privPass) : null
          );

          // Add user by security name (will be auto-localized per sender engineID)
          usm.addUser(usmUser.getSecurityName(), usmUser);

          // Log summary (no secrets)
          String mode =
                  (authProtocol == null && privProtocol == null) ? "noAuthNoPriv" :
                          (authProtocol != null && privProtocol == null) ? "authNoPriv" : "authPriv";
          System.out.println("[SNMP] v3 USM user configured: user='" + user + "', mode=" + mode +
                  (authProtocol != null ? ", auth=" + authProtocol : "") +
                  (privProtocol != null ? ", priv=" + privProtocol : ""));
        }
      }

      snmp.addCommandResponder(this);

      // Start listening
      transport.listen();
      System.out.println("[SNMP] Trap server listening on UDP 0.0.0.0:" + port + " (IPv4)");

      // Optional: also listen on IPv6 if your sender uses it
      try {
        TransportMapping<?> transport6 = new DefaultUdpTransportMapping(new UdpAddress("[::]/" + port));
        snmp.addTransportMapping(transport6);
        transport6.listen();
        System.out.println("[SNMP] Trap server listening on UDP [::]:" + port + " (IPv6)");
      } catch (IOException ipv6BindEx) {
        System.out.println("[SNMP] IPv6 listen skipped: " + ipv6BindEx.getMessage());
      }

      running.set(true);
    } catch (IOException e) {
      throw new RuntimeException("Failed to start SNMP trap server on UDP " + port, e);
    }
  }

  // Helper: map string to auth OID (MD5, SHA1, SHA-2 families)
  private static OID toAuthOID(String name) {
    if (name == null) return null;
    String n = name.trim().toUpperCase();
    switch (n) {
      case "MD5":
        return AuthMD5.ID;
      case "SHA":
      case "SHA1":
        return AuthSHA.ID;
      case "SHA224":
        return AuthHMAC128SHA224.ID;
      case "SHA256":
        return AuthHMAC192SHA256.ID;
      case "SHA384":
        return AuthHMAC256SHA384.ID;
      case "SHA512":
        return AuthHMAC384SHA512.ID;
      default:
        System.out.println("[SNMP] Unknown v3 authentication protocol: " + name + " (falling back to noAuth)");
        return null;
    }
  }

  // Helper: map string to privacy OID (DES, AES variants)
  private static OID toPrivOID(String name) {
    if (name == null) return null;
    String n = name.trim().toUpperCase();
    switch (n) {
      case "DES":
        return PrivDES.ID;
      case "AES":
      case "AES128":
        return PrivAES128.ID;
      case "AES192":
        return PrivAES192.ID;
      case "AES256":
        return PrivAES256.ID;
      default:
        System.out.println("[SNMP] Unknown v3 privacy protocol: " + name + " (falling back to noPriv)");
        return null;
    }
  }

  private static String trimToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
  public synchronized void stop() {
    running.set(false);
    try { if (snmp != null) snmp.close(); } catch (Exception ignored) {}
    try { if (transport != null) transport.close(); } catch (Exception ignored) {}
  }

  public boolean isRunning() { return running.get(); }
  
  public int getPort() { return port; }

  // Java
  @Override
  public void processPdu(CommandResponderEvent event) {
    try {
      var pdu = event.getPDU();
      if (pdu == null) return;

      // Determine version from message processing model
      String version;
      int mpModel = event.getMessageProcessingModel();
      if (mpModel == MPv1.ID) {
        version = "1";
      } else if (mpModel == MPv2c.ID) {
        version = "2c";
      } else if (mpModel == org.snmp4j.mp.MPv3.ID) {
        version = "3";
      } else {
        version = "unknown";
      }

      // Community (v1/v2c) or username (v3)
      String communityOrUser = event.getSecurityName() != null ? event.getSecurityName().toString() : null;

      // Remote address as host:port
      String remoteAddress = null;
      if (event.getPeerAddress() instanceof UdpAddress addr) {
        var inet = addr.getInetAddress();
        String host = inet != null ? inet.getHostAddress() : addr.getInetAddress().toString();
        remoteAddress = host + ":" + addr.getPort();
      } else if (event.getPeerAddress() != null) {
        // Fallback
        remoteAddress = event.getPeerAddress().toString();
      }

      // Collect variables and determine trap OID
      final String OID_SNMP_TRAP_OID = "1.3.6.1.6.3.1.1.4.1.0"; // snmpTrapOID.0 (v2/v3)
      final String OID_SNMP_TRAPS_BASE = "1.3.6.1.6.3.1.1.5";   // snmpTraps base

      List<Var> variables = new ArrayList<>();
      String trapOid = null;

      for (var vb : pdu.getVariableBindings()) {
        String oidStr = vb.getOid().toDottedString();
        String valueStr = vb.getVariable().toString();

        // v2c/v3 trap OID
        if (OID_SNMP_TRAP_OID.equals(oidStr) && vb.getVariable() instanceof OID) {
          trapOid = ((OID) vb.getVariable()).toDottedString();
        }

        variables.add(new Var(oidStr, valueStr));
      }

      // v1 trap OID derivation if needed
      if (trapOid == null && pdu instanceof PDUv1 v1) {
        String enterprise = v1.getEnterprise() != null ? v1.getEnterprise().toDottedString() : null;
        int generic = v1.getGenericTrap();
        int specific = v1.getSpecificTrap();

        if (generic == PDUv1.ENTERPRISE_SPECIFIC) {
          if (enterprise != null) {
            trapOid = enterprise + ".0." + specific;
          }
        } else {
          // Map generic 0..5 -> snmpTraps.1..6
          trapOid = OID_SNMP_TRAPS_BASE + "." + (generic + 1);
        }
      }

      // Build the TrapMessage record with real values
      TrapMessage msg = new TrapMessage(
              Instant.now(),
              remoteAddress,
              version,
              communityOrUser,
              trapOid,
              variables,
              mib // keep the supplier passed to this server
      );

      dispatcher.accept(msg);
      event.setProcessed(true);
    } catch (Exception ex) {
      // Make sure one bad trap does not break the responder
      ex.printStackTrace();
    }
  }
}
