package ar.qubi.demo;

import ar.qubi.snmp.traps.SnmpTrapListener;
import ar.qubi.snmp.api.TrapMessage;
import org.springframework.stereotype.Component;


@Component
class TrapLogger {
    @SnmpTrapListener
    public void logTrap(TrapMessage msg) {
        System.out.println("Trap received: " + msg);
    }
}



