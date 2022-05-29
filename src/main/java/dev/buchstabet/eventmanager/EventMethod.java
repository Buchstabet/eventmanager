package dev.buchstabet.eventmanager;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class EventMethod {

  private final Method method;
  private final Class<? extends Event> event;
  private final Object instance;


}
