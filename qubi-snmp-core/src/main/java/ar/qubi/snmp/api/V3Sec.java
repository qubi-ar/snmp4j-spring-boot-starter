package ar.qubi.snmp.api;
public record V3Sec(String username, String securityLevel, String authProtocol, String authPass,
                    String privProtocol, String privPass) {}
