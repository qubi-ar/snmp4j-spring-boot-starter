package ar.qubi.demo;

import ar.qubi.snmp.traps.SnmpTrapListener;
import ar.qubi.snmp.api.TrapMessage;
import org.springframework.stereotype.Component;

@Component
public class TrapLogger {
  @SnmpTrapListener
  public void onTrap(TrapMessage msg) {
    System.out.println("=== TRAP RECEIVED ===");
    System.out.println("Time: " + msg.receivedAt());
    System.out.println("From: " + msg.remoteAddress());
    System.out.println("Version: " + msg.version());
    System.out.println("Community: " + msg.communityOrUser());
    System.out.println("OID: " + msg.trapOid());
    System.out.println("Variables: " + msg.variables());
    System.out.println("====================");
  }
}
