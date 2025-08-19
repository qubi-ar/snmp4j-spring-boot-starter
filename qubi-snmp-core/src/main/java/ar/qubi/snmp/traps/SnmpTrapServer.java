package ar.qubi.snmp.traps;

import ar.qubi.snmp.autoconfigure.QubiSnmpProperties;
import ar.qubi.snmp.mib.MibLookup;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SnmpTrapServer {
    private final int port;
    private final TrapDispatcher dispatcher;
    private final Supplier<MibLookup> mibLookup;
    private SnmpTrapServerSnmp4j server;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final QubiSnmpProperties.Traps.Security security;
    public SnmpTrapServer(int port, QubiSnmpProperties.Traps.Security security, Supplier<MibLookup> mibLookup,TrapDispatcher dispatcher) {
        this.port = port;
        this.security = security;
        this.mibLookup = mibLookup;
        this.dispatcher = dispatcher;

    }


    public synchronized void start() {
        if (running.get()) {
            return;
        }
        
        server = new SnmpTrapServerSnmp4j(this.security, port, dispatcher, mibLookup);
        server.start();
        running.set(true);
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }
        
        if (server != null) {
            server.stop();
            server = null;
        }
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getPort() {
        return port;
    }

    public TrapDispatcher getDispatcher() {
        return dispatcher;
    }
}