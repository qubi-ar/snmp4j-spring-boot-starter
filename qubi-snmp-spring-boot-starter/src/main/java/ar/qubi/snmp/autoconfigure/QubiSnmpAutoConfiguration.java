package ar.qubi.snmp.autoconfigure;

import ar.qubi.snmp.api.TrapMessage;
import ar.qubi.snmp.client.SnmpClient;
import ar.qubi.snmp.client.SnmpClientSnmp4j;

import ar.qubi.snmp.mib.MibLookup;
import ar.qubi.snmp.mib.SystemMibLookup;
import ar.qubi.snmp.scheduler.JobConfig;
import ar.qubi.snmp.scheduler.PollingJob;
import ar.qubi.snmp.scheduler.SnmpPollingRegistry;
import ar.qubi.snmp.scheduler.SnmpPollingRegistryInMemory;
import ar.qubi.snmp.traps.SnmpTrapListener;
import ar.qubi.snmp.traps.SnmpTrapServer;
import ar.qubi.snmp.traps.TrapDispatcher;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

@AutoConfiguration
@EnableConfigurationProperties(QubiSnmpProperties.class)
@ConditionalOnProperty(prefix = "qubi.snmp", name = "enabled", havingValue = "true", matchIfMissing = true)
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
  public SnmpClient snmpClient() throws IOException {
    return new SnmpClientSnmp4j();
  }

  @Bean(destroyMethod = "shutdown")
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
  public SnmpTrapServer snmpTrapServer(QubiSnmpProperties props, MibLookup mibLookup, TrapDispatcher dispatcher) {
    return new SnmpTrapServer(
            props.getTraps().getPort(),
            props.getTraps().getSecurity(),
            () -> mibLookup,
            dispatcher
            );
  }

  @Bean
  public SmartInitializingSingleton snmpInitializer(ApplicationContext ctx) {
    return () -> {
      var log = Logger.getLogger("qubi");

      TrapDispatcher dispatcher;
      try {
        dispatcher = ctx.getBean(TrapDispatcher.class);
      } catch (Exception e) {
        log.info("No TrapDispatcher found");
        return;
      }

      for (String name : ctx.getBeanDefinitionNames()) {
        Object bean;
        try {
          bean = ctx.getBean(name);
        } catch (Exception e) {
          continue;
        }
        Class<?> targetClass = ClassUtils.getUserClass(bean);
        for (var m : targetClass.getMethods()) {
          if (m.isAnnotationPresent(SnmpTrapListener.class)
                  && m.getParameterCount() == 1
                  && TrapMessage.class.isAssignableFrom(m.getParameterTypes()[0])) {

            var method = m;
            dispatcher.add(msg -> {
              try {
                method.invoke(bean, msg);
              } catch (Exception ex) {
                log.log(Level.WARNING,
                        "Error invoking @SnmpTrapListener " + targetClass.getSimpleName() + "#" + method.getName(),
                        ex);
              }
            });
            log.info("Registered @SnmpTrapListener -> "
                    + targetClass.getSimpleName() + "#" + method.getName());
          }
        }
      }
      log.info("Trap listeners registered: " + dispatcher.size());

      try {
        SnmpPollingRegistry registry = ctx.getBean(SnmpPollingRegistry.class);
        QubiSnmpProperties props = ctx.getBean(QubiSnmpProperties.class);
        var jobs = ctx.getBeansOfType(PollingJob.class);
        for (PollingJob job : jobs.values()) {
          String cronExpression = props.getScheduler().getCron();
          Duration fixedRate = cronExpression != null && !cronExpression.trim().isEmpty() 
              ? null 
              : Duration.ofSeconds(props.getScheduler().getFixedRateSeconds());
          
          String jobId = registry.register(
                  job,
                  new JobConfig(cronExpression, fixedRate, null, java.util.Map.of())
          );
          
          String scheduleInfo = cronExpression != null && !cronExpression.trim().isEmpty()
              ? "cron: " + cronExpression
              : "fixed rate: " + props.getScheduler().getFixedRateSeconds() + "s";
          log.info("Registered polling job: " + job.name() + " (" + scheduleInfo + ", ID: " + jobId + ")");
        }
      } catch (Exception e) {
        log.info("No polling registry found: " + e.getMessage());
      }

      try {
        SnmpTrapServer trapServer = ctx.getBean(SnmpTrapServer.class);
        trapServer.start();
        log.info("SNMP trap server started on port " + trapServer.getPort());
      } catch (Exception e) {
        log.info("No SNMP trap server configured or failed to start: " + e.getMessage());
      }
    };
  }
}