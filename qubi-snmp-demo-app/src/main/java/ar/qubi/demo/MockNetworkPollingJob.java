package ar.qubi.demo;

import ar.qubi.snmp.api.TargetSpec;
import ar.qubi.snmp.api.V3Sec;
import ar.qubi.snmp.scheduler.JobContext;
import ar.qubi.snmp.scheduler.PollingJob;
import org.springframework.stereotype.Component;

@Component
public class MockNetworkPollingJob implements PollingJob {

  @Override
  public String name() {
    return "mock-network-poller";
  }

  @Override
  public void execute(JobContext ctx) throws Exception {
    var logger = ctx.logger();
    var client = ctx.client();
    
    // Poll router1 on port 1611
    TargetSpec router1 = new TargetSpec("127.0.0.1", 1611, "2c", "public", null);
    var router1Result = client.get(router1, "1.3.6.1.2.1.1.5.0", "1.3.6.1.2.1.1.3.0");
    
    if (router1Result.ok()) {
      System.out.println("[POLL] Router1 SNMP data: " + router1Result.vars());
      logger.info("Router1 SNMP data: " + router1Result.vars());
    } else {
      System.out.println("[POLL] Failed to collect from router1: " + router1Result.error());
      logger.warning("Failed to collect from router1: " + router1Result.error());
    }

    // Poll core-switch on port 1612
    TargetSpec coreSwitch = new TargetSpec("127.0.0.1", 1612, "2c", "monitor",null );
    var switchResult = client.get(coreSwitch, "1.3.6.1.2.1.2.2.1.8.1", "1.3.6.1.2.1.1.3.0");
    
    if (switchResult.ok()) {
      System.out.println("[POLL] Core-switch SNMP data: " + switchResult.vars());
      logger.info("Core-switch SNMP data: " + switchResult.vars());
    } else {
      System.out.println("[POLL] Failed to collect from core-switch: " + switchResult.error());
      logger.warning("Failed to collect from core-switch: " + switchResult.error());
    }
  }
}