package ar.qubi.snmp.traps;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SnmpTrapListener {
  String[] oids() default {};
  String version() default "";
  String community() default "";
  String username() default "";
}
