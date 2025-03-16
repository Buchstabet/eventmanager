package dev.buchstabet.eventmanager;

import lombok.Getter;
import org.w3c.dom.events.EventException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EventLoader
{

  @Getter private final List<EventMethod> eventClasses;
  private final ExecutorService executorService;

  public EventLoader(List<EventMethod> eventClasses, ExecutorService executorService)
  {
    this.executorService = executorService;
    this.eventClasses = new ArrayList<>();
    eventClasses.forEach(this::register);
  }

  public EventLoader(ExecutorService executorService)
  {
    this(new ArrayList<>(), executorService);
  }

  public EventLoader()
  {
    this(Executors.newSingleThreadExecutor(r -> {
      Thread thread = new Thread(r, "EventManager");
      thread.setDaemon(true);
      return thread;
    }));
  }

  /**
   * @param v The event that is to be triggered.
   */
  public <V extends Event> void throwEvent(V v, Predicate<EventMethod> predicate) throws EventException
  {
    throwEventStream(new ArrayList<>(eventClasses).stream().filter(predicate), v);
  }

  /**
   * @param v The event that is to be triggered.
   */
  public <V extends Event> void throwEvent(V v) throws EventException
  {
    throwEventStream(new ArrayList<>(eventClasses).stream(), v);

  }

  private <V extends Event> void throwEventStream(Stream<EventMethod> stream, V v)
  {
    stream.filter(eventMethod -> eventMethod.getEvent().isInstance(v)).forEach(eventMethod -> {
      Runnable r = () -> {
        try {
          eventMethod.getMethod().invoke(eventMethod.getInstance(), v);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }

      };
      if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) r.run();
      else executorService.execute(r);
    });
  }

  public void register(Object o) throws EventException
  {
    register(o, o.getClass());
  }

  public void register(Object o, Method[] methods) throws EventException
  {
    List<EventMethod> collect = Arrays.stream(methods).filter(method -> method.getParameterTypes().length == 1)
            .filter(method -> checkForEvent(method.getParameterTypes()[0]))
            .filter(method -> !Modifier.isAbstract(method.getModifiers()))
            .filter(method -> method.isAnnotationPresent(EventManager.class))
            .map(method -> new EventMethod(method, (Class<? extends Event>) method.getParameterTypes()[0], o)).toList();
    eventClasses.addAll(collect);
  }

  private boolean checkForEvent(Class<?> parameterType)
  {
    return Event.class.isAssignableFrom(parameterType);
  }

  public void register(Object o, Class<?> clazz)
  {
    List<Class<?>> classesToHandle = new ArrayList<>();
    do {
      classesToHandle.add(clazz);
      clazz = clazz.getSuperclass();
    } while (clazz != null && !clazz.equals(Object.class));

    classesToHandle.forEach(aClass -> register(o, aClass.getDeclaredMethods()));
  }

  public void registerAll(Object... o)
  {
    for (Object object : o) {
      register(object);
    }
  }


  public void unregister(Object instance)
  {
    eventClasses.removeIf(eventMethod -> eventMethod.getInstance().equals(instance));
  }
}
