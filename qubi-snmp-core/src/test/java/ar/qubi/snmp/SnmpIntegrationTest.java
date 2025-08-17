package ar.qubi.snmp;

import ar.qubi.snmp.api.*;
import ar.qubi.snmp.client.SnmpClientSnmp4j;
import ar.qubi.snmp.mib.SystemMibLookup;
import ar.qubi.snmp.traps.SnmpTrapServer;
import ar.qubi.snmp.traps.TrapDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SnmpIntegrationTest {
    
    private SnmpClientSnmp4j client;
    private SnmpTrapServer trapServer;
    private TrapDispatcher trapDispatcher;
    private SystemMibLookup mibLookup;
    
    @BeforeEach
    void setUp() throws IOException {
        client = new SnmpClientSnmp4j();
        trapDispatcher = new TrapDispatcher();
        mibLookup = new SystemMibLookup();
        trapServer = new SnmpTrapServer(1162, trapDispatcher, () -> mibLookup);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
        if (trapServer != null && trapServer.isRunning()) {
            trapServer.stop();
        }
    }
    
    @Test
    void testClientCreation() {
        assertNotNull(client);
    }
    
    @Test
    void testSnmpGetWithInvalidTarget() {
        TargetSpec target = new TargetSpec("192.168.1.999", 161, "2c", "public", null);
        
        assertThrows(IllegalArgumentException.class, () -> {
            client.get(target, "1.3.6.1.2.1.1.1.0");
        });
    }
    
    @Test
    void testSnmpWalkWithInvalidTarget() {
        TargetSpec target = new TargetSpec("192.168.1.999", 161, "2c", "public", null);
        SnmpWalkResult result = client.walk(target, "1.3.6.1.2.1.1");
        
        assertFalse(result.ok());
        assertNotNull(result.error());
        assertTrue(result.error().contains("Invalid host address"));
        assertTrue(result.durationMs() >= 0);
    }
    
    @Test
    void testTrapServerStartStop() {
        assertFalse(trapServer.isRunning());
        
        trapServer.start();
        assertTrue(trapServer.isRunning());
        assertEquals(1162, trapServer.getPort());
        
        trapServer.stop();
        assertFalse(trapServer.isRunning());
    }
    
    @Test
    void testTrapDispatcher() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        
        trapDispatcher.add(trapMessage -> {
            assertNotNull(trapMessage);
            latch.countDown();
        });
        
        assertEquals(1, trapDispatcher.size());
        
        // Simulate trap message
        TrapMessage testTrap = new TrapMessage(
            java.time.Instant.now(),
            "192.168.1.100:161",
            "2c",
            "public",
            "1.3.6.1.6.3.1.1.5.3",
            java.util.List.of(new Var("1.3.6.1.2.1.1.3.0", "12345")),
            () -> mibLookup
        );
        
        trapDispatcher.accept(testTrap);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
    
    @Test
    void testMibLookup() {
        // Test system description lookup
        var result = mibLookup.find("1.3.6.1.2.1.1.1.0");
        assertTrue(result.isPresent());
        assertEquals("sysDescr", result.get().name());
        assertEquals("System description", result.get().description());
        
        // Test interface index lookup
        result = mibLookup.find("1.3.6.1.2.1.2.2.1.1");
        assertTrue(result.isPresent());
        assertEquals("ifIndex", result.get().name());
        
        // Test table instance lookup (should find parent)
        result = mibLookup.find("1.3.6.1.2.1.2.2.1.1.1");
        assertTrue(result.isPresent());
        assertEquals("ifIndex.1", result.get().name());
        
        // Test unknown OID
        result = mibLookup.find("1.2.3.4.5.6.7.8.9.0");
        assertFalse(result.isPresent());
        
        // Test null OID
        result = mibLookup.find(null);
        assertFalse(result.isPresent());
        
        // Test that we have common system MIB entries
        assertTrue(mibLookup.getAllEntries().size() > 0);
        assertTrue(mibLookup.getAllEntries().containsKey("1.3.6.1.2.1.1.1.0"));
    }
    
    @Test
    void testTargetSpecCreation() {
        // Test SNMPv2c target
        TargetSpec v2Target = new TargetSpec("localhost", 161, "2c", "public", null);
        assertEquals("localhost", v2Target.host());
        assertEquals(161, v2Target.port());
        assertEquals("2c", v2Target.version());
        assertEquals("public", v2Target.community());
        assertNull(v2Target.v3());
        
        // Test SNMPv3 target
        V3Sec v3Sec = new V3Sec("testuser", "authpriv", "SHA", "authpass", "AES", "privpass");
        TargetSpec v3Target = new TargetSpec("localhost", 161, "3", null, v3Sec);
        assertEquals("3", v3Target.version());
        assertNotNull(v3Target.v3());
        assertEquals("testuser", v3Target.v3().username());
        assertEquals("authpriv", v3Target.v3().securityLevel());
    }
    
    @Test
    void testSnmpResultProcessing() {
        java.util.List<Var> vars = java.util.List.of(
            new Var("1.3.6.1.2.1.1.1.0", "Test System"),
            new Var("1.3.6.1.2.1.1.5.0", "TestHost")
        );
        
        SnmpResult result = new SnmpResult(vars, 100L, null);
        assertTrue(result.ok());
        assertEquals(2, result.vars().size());
        assertEquals(100L, result.durationMs());
        
        var map = result.asMap();
        assertEquals("Test System", map.get("1.3.6.1.2.1.1.1.0"));
        assertEquals("TestHost", map.get("1.3.6.1.2.1.1.5.0"));
        
        // Test error result
        SnmpResult errorResult = new SnmpResult(java.util.List.of(), 50L, "Timeout");
        assertFalse(errorResult.ok());
        assertEquals("Timeout", errorResult.error());
        assertTrue(errorResult.asMap().isEmpty());
    }
}