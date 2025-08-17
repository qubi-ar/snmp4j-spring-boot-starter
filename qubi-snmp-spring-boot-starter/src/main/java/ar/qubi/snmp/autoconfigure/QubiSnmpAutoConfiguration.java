package ar.qubi.snmp.autoconfigure;

import ar.qubi.snmp.api.TrapMessage;
import ar.qubi.snmp.autoconfigure.QubiSnmpProperties;
import ar.qubi.snmp.client.SnmpClient;
import ar.qubi.snmp.client.SnmpClientStub;
import ar.qubi.snmp.mib.MibLookup;
import ar.qubi.snmp.mib.SystemMibLookup;
import ar.qubi.snmp.scheduler.JobConfig;
import ar.qubi.snmp.scheduler.PollingJob;
import ar.qubi.snmp.scheduler.SnmpPollingRegistry;
import ar.qubi.snmp.scheduler.SnmpPollingRegistryInMemory;
import ar.qubi.snmp.traps.SnmpTrapListener;
import ar.qubi.snmp.traps.SnmpTrapServerSnmp4j;
import ar.qubi.snmp.traps.TrapDispatcher;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@Configuration
@EnableConfigurationProperties(QubiSnmpProperties.class)
public class QubiSnmpAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public MibLookup mibLookup() {
    return new SystemMibLookup();
  }

  @Bean
  @ConditionalOnMissingBean
  public TrapDispatcher trapDispatcher() {
    return new TrapDispatcher();
  }

  @Bean
  @ConditionalOnMissingBean
  public SnmpClient snmpClient() {
    return new SnmpClientStub();
  }

  @Bean
  @ConditionalOnMissingBean
  public ScheduledExecutorService scheduledExecutorService() {
    return Executors.newScheduledThreadPool(4);
  }

  @Bean
  @ConditionalOnMissingBean
  public SnmpPollingRegistry snmpPollingRegistry(ScheduledExecutorService executor, SnmpClient client) {
    return new SnmpPollingRegistryInMemory(executor, client);
  }

  @Bean
  @ConditionalOnProperty(
          prefix = "qubi.snmp.traps",
          name = "enabled",
          havingValue = "true",
          matchIfMissing = true
  )
  public SnmpTrapServerSnmp4j snmpTrapServer(
          QubiSnmpProperties props,
          MibLookup mibLookup,
          TrapDispatcher dispatcher) {

    var server = new SnmpTrapServerSnmp4j(
            props.getTraps().getPort(),
            dispatcher,
            () -> mibLookup
    );
    return server;
  }

  @Bean
  public SmartInitializingSingleton snmpInitializer(ApplicationContext ctx) {
    return () -> {
      var log = Logger.getLogger("qubi");
      
      // Get the trap dispatcher
      TrapDispatcher dispatcher;
      try {
        dispatcher = ctx.getBean(TrapDispatcher.class);
      } catch (Exception e) {
        log.info("No TrapDispatcher found");
        return;
      }
      
      // Register trap listeners first
      for (String name : ctx.getBeanDefinitionNames()) {
        Object bean;
        try { bean = ctx.getBean(name); } catch (Exception e) { continue; }
        for (var m : bean.getClass().getMethods()) {
          if (m.isAnnotationPresent(SnmpTrapListener.class)
                  && m.getParameterCount() == 1
                  && m.getParameterTypes()[0].equals(TrapMessage.class)) {

            var target = bean;
            var method = m;
            dispatcher.add(msg -> {
              try { method.invoke(target, msg); }
              catch (Exception ignored) {}
            });
            log.info("Registered @SnmpTrapListener -> "
                    + target.getClass().getSimpleName() + "#" + method.getName());
          }
        }
      }
      log.info("Trap listeners registered: " + dispatcher.size());
      
      // Register polling jobs
      try {
        SnmpPollingRegistry registry = ctx.getBean(SnmpPollingRegistry.class);
        for (String name : ctx.getBeanDefinitionNames()) {
          Object bean;
          try { bean = ctx.getBean(name); } catch (Exception e) { continue; }
          if (bean instanceof PollingJob job) {
            String jobId = registry.register(job, new JobConfig(null, Duration.ofSeconds(10), null, Map.of()));
            log.info("Registered polling job: " + job.name() + " (ID: " + jobId + ")");
          }
        }
      } catch (Exception e) {
        log.info("No polling registry found: " + e.getMessage());
      }
      
      // Start the trap server if it exists
      try {
        SnmpTrapServerSnmp4j trapServer = ctx.getBean(SnmpTrapServerSnmp4j.class);
        trapServer.start();
        log.info("SNMP trap server started on port " + trapServer.getPort());
      } catch (Exception e) {
        log.info("No SNMP trap server configured or failed to start: " + e.getMessage());
      }
    };
  }
}
