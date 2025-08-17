package ar.qubi.snmp.traps;

import ar.qubi.snmp.api.TrapMessage;
import ar.qubi.snmp.api.Var;
import ar.qubi.snmp.mib.MibLookup;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.InetSocketAddress;
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

  public SnmpTrapServerSnmp4j(int port, Consumer<TrapMessage> dispatcher, Supplier<MibLookup> mib) {
    this.port = port;
    this.dispatcher = dispatcher;
    this.mib = mib;
  }

  public synchronized void start() {
    if (running.get()) return;
    try {
      transport = new DefaultUdpTransportMapping(new UdpAddress(port));
      snmp = new Snmp(transport);
      snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
      snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
      snmp.addCommandResponder(this);
      transport.listen();
      running.set(true);
    } catch (IOException e) {
      throw new RuntimeException("Failed to start SNMP trap server on UDP " + port, e);
    }
  }

  public synchronized void stop() {
    running.set(false);
    try { if (snmp != null) snmp.close(); } catch (Exception ignored) {}
    try { if (transport != null) transport.close(); } catch (Exception ignored) {}
  }

  public boolean isRunning() { return running.get(); }
  
  public int getPort() { return port; }

  @Override
  public void processPdu(CommandResponderEvent event) {
    try {
      var pdu = event.getPDU();
      if (pdu == null) return;
      List<Var> vars = new ArrayList<>();
      pdu.getVariableBindings().forEach(vb ->
          vars.add(new Var(vb.getOid().toDottedString(), vb.getVariable().toString()))
      );

      String trapOid;
      if (pdu.getVariableBindings().size() > 0) {
        // Buscar snmpTrapOID.0 o enterprise OID (v1/v2c simplificado)
        OID snmpTrapOid = new OID("1.3.6.1.6.3.1.1.4.1.0");
        var found = pdu.getVariableBindings().stream()
            .filter(vb -> vb.getOid().equals(snmpTrapOid))
            .findFirst();
        trapOid = found.map(vb -> vb.getVariable().toString()).orElse("1.3.6.1.6.3.1.1.5"); // generic-notifs prefix
      } else {
        trapOid = "unknown";
      }

      String remote = "unknown";
      if (event.getPeerAddress() instanceof UdpAddress addr) {
        InetSocketAddress isa = new InetSocketAddress(addr.getInetAddress(), addr.getPort());
        remote = isa.getAddress().getHostAddress() + ":" + isa.getPort();
      }

      var msg = new TrapMessage(
          Instant.now(),
          remote,
          "v2c",
          "public",
          trapOid,
          vars,
          mib
      );
      dispatcher.accept(msg);
    } catch (Exception e) {
      System.err.println("Error processing trap: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
