package ar.qubi.snmp.scheduler;

import java.util.Map;
import java.util.logging.Logger;
import ar.qubi.snmp.client.SnmpClient;

public interface JobContext {
  Map<String,Object> params();
  SnmpClient client();
  Logger logger();
  void recordMetric(String key, double value);
}
