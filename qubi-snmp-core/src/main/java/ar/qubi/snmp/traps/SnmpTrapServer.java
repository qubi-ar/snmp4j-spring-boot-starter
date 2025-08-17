package ar.qubi.snmp.traps;

import ar.qubi.snmp.api.TrapMessage;
import ar.qubi.snmp.mib.MibLookup;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SnmpTrapServer {
    private final int port;
    private final TrapDispatcher dispatcher;
    private final Supplier<MibLookup> mibLookup;
    private SnmpTrapServerSnmp4j server;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SnmpTrapServer(int port, TrapDispatcher dispatcher, Supplier<MibLookup> mibLookup) {
        this.port = port;
        this.dispatcher = dispatcher;
        this.mibLookup = mibLookup;
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        
        server = new SnmpTrapServerSnmp4j(port, dispatcher, mibLookup);
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