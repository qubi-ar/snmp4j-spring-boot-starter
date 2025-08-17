bash -c '
set -euo pipefail

# 1) CORE: Dispatcher mutable
cat > qubi-snmp-core/src/main/java/ar/qubi/snmp/traps/TrapDispatcher.java <<JAVA
package ar.qubi.snmp.traps;

import ar.qubi.snmp.api.TrapMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class TrapDispatcher implements Consumer<TrapMessage> {
  private final List<Consumer<TrapMessage>> consumers = new CopyOnWriteArrayList<>();
  public void add(Consumer<TrapMessage> c) { if (c != null) consumers.add(c); }
  public void remove(Consumer<TrapMessage> c) { consumers.remove(c); }
  @Override public void accept(TrapMessage msg) {
    for (var c : consumers) {
      try { c.accept(msg); } catch (Exception ignored) {}
    }
  }
  public int size() { return consumers.size(); }
}
JAVA

# 2) STARTER: AutoConfig sin escaneo dentro del bean del server
cat > qubi-snmp-spring-boot-starter/src/main/java/ar/qubi/snmp/autoconfigure/QubiSnmpAutoConfiguration.java <<JAVA
package ar.qubi.snmp.autoconfigure;

import ar.qubi.snmp.api.TrapMessage;
import ar.qubi.snmp.client.SnmpClient;
import ar.qubi.snmp.client.SnmpClientStub;
import ar.qubi.snmp.mib.MibLookup;
import ar.qubi.snmp.mib.NoopMibLookup;
import ar.qubi.snmp.scheduler.SnmpPollingRegistry;
import ar.qubi.snmp.scheduler.SnmpPollingRegistryInMemory;
import ar.qubi.snmp.traps.SnmpTrapListener;
import ar.qubi.snmp.traps.SnmpTrapServerSnmp4j;
import ar.qubi.snmp.traps.TrapDispatcher;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

@Configuration
@EnableConfigurationProperties(QubiSnmpProperties.class)
@ConditionalOnProperty(prefix="qubi.snmp", name="enabled", havingValue="true", matchIfMissing=true)
public class QubiSnmpAutoConfiguration implements ApplicationContextAware {
  private ApplicationContext ctx;
  private final Logger log = Logger.getLogger(getClass().getName());

  @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.ctx = applicationContext;
  }

  @Bean @ConditionalOnMissingBean
  public MibLookup mibLookup() { return new NoopMibLookup(); }

  @Bean @ConditionalOnMissingBean
  public SnmpClient snmpClient() { return new SnmpClientStub(); }

  @Bean @ConditionalOnMissingBean
  public ScheduledExecutorService snmpSchedulerExecutor() {
    return Executors.newScheduledThreadPool(2);
  }

  @Bean @ConditionalOnMissingBean
  public SnmpPollingRegistry snmpPollingRegistry(ScheduledExecutorService exec, SnmpClient client) {
    return new SnmpPollingRegistryInMemory(exec, client);
  }

  /** Dispatcher mutable para listeners anotados */
  @Bean @ConditionalOnMissingBean
  public TrapDispatcher trapDispatcher() { return new TrapDispatcher(); }

  /** Server real: NO escanea beans; sólo arranca y despacha usando el dispatcher */
  @Bean
  @ConditionalOnProperty(prefix="qubi.snmp.traps", name="enabled", havingValue="true", matchIfMissing=true)
  public SnmpTrapServerSnmp4j snmpTrapServer(QubiSnmpProperties props, MibLookup mibLookup, TrapDispatcher dispatcher) {
    var server = new SnmpTrapServerSnmp4j(props.getTraps().getPort(), dispatcher, () -> mibLookup);
    server.start();
    log.info("SNMP Trap server started on UDP " + props.getTraps().getPort());
    return server;
  }

  /**
   * Registrar que corre DESPUÉS de instanciar todos los singletons,
   * encuentra métodos @SnmpTrapListener y los agrega al dispatcher.
   */
  @Bean
  public SmartLifecycle trapListenerRegistrar(TrapDispatcher dispatcher) {
    return new SmartLifecycle() {
      private volatile boolean running = false;

      @Override public void start() {
        // Scan liviano: iteramos beans y registramos métodos @SnmpTrapListener
        for (String name : ctx.getBeanDefinitionNames()) {
          Object bean = null;
          try { bean = ctx.getBean(name); } catch (Exception ignored) { continue; }
          for (var m : bean.getClass().getMethods()) {
            if (m.isAnnotationPresent(SnmpTrapListener.class)
                && m.getParameterCount() == 1
                && m.getParameterTypes()[0].equals(TrapMessage.class)) {
              var target = bean;
              var method = m;
              dispatcher.add(msg -> {
                try { method.invoke(target, msg); } catch (Exception ignored) {}
              });
              log.info("Registered @SnmpTrapListener -> " + target.getClass().getSimpleName() + "#" + method.getName());
            }
          }
        }
        running = true;
        log.info("Trap listeners registered: " + dispatcher.size());
      }

      @Override public void stop() { running = false; }
      @Override public boolean isRunning() { return running; }
      @Override public int getPhase() { return Integer.MAX_VALUE; } // correr tarde
      @Override public boolean isAutoStartup() { return true; }
      @Override public void stop(Runnable callback) { stop(); callback.run(); }
    };
  }
}
JAVA

echo "Parche aplicado. Recompilá:"
echo "  mvn -q -DskipTests clean install"
echo "Corré la demo:"
echo "  cd qubi-snmp-demo-app && mvn spring-boot:run"
'
