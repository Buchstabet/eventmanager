package dev.buchstabet.eventmanager;


import lombok.Getter;
import org.w3c.dom.events.EventException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventLoader {

  @Getter private final List<EventMethod> eventClasses;

  public EventLoader(List<EventMethod> eventClasses) {
    this.eventClasses = new ArrayList<>();
    eventClasses.forEach(this::register);
  }

  public EventLoader() {
    this(new ArrayList<>());
  }

  public <V extends Event> void throwEvent(V v) {
    eventClasses.stream().filter(eventMethod -> eventMethod.getEvent().isInstance(v)).forEach(eventMethod -> {
      try {
        eventMethod.getMethod().invoke(eventMethod.getInstance(), v);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  public void register(Object o) throws EventException {
    List<EventMethod> collect = Arrays.stream(o.getClass().getDeclaredMethods()).
            filter(method -> method.getParameterTypes().length == 1).
            filter(method -> Arrays.asList(method.getParameterTypes()[0].getInterfaces()).contains(Event.class)).
            map(method -> new EventMethod(method, (Class<? extends Event>) method.getParameterTypes()[0], o)).
            collect(Collectors.toList());
    eventClasses.addAll(collect);
  }



}
