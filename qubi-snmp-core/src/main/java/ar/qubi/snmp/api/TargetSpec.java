package ar.qubi.snmp.api;
public record TargetSpec(String host, int port, String version, String community, V3Sec v3) {}
