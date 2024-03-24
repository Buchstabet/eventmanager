package dev.buchstabet.eventmanager;


import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.w3c.dom.events.EventException;

public class EventLoader
{

  @Getter private final List<EventMethod> eventClasses;

  public EventLoader(List<EventMethod> eventClasses)
  {
    this.eventClasses = new ArrayList<>();
    eventClasses.forEach(this::register);
  }

  public EventLoader()
  {
    this(new ArrayList<>());
  }

  /**
   * @param v The event that is to be triggered.
   */
  public <V extends Event> void throwEvent(V v)
  {
    new ArrayList<>(eventClasses).stream()
        .filter(eventMethod -> eventMethod.getEvent().isInstance(v)).forEach(eventMethod ->
        {
          try
          {
            eventMethod.getMethod().invoke(eventMethod.getInstance(), v);
          } catch (Exception e)
          {
            e.printStackTrace();
          }
        });
  }


  public void register(Object o) throws EventException
  {
    register(o, o.getClass());
  }

  public void register(Object o, Method[] methods) throws EventException
  {
    List<EventMethod> collect =
        Arrays.stream(methods).filter(method -> method.getParameterTypes().length == 1)
            .filter(method -> checkForEvent(method.getParameterTypes()[0]))
            .filter(method -> !Modifier.isAbstract(method.getModifiers()))
            .filter(method -> method.isAnnotationPresent(EventManager.class)).map(
                method -> new EventMethod(method,
                    (Class<? extends Event>) method.getParameterTypes()[0], o)).toList();
    eventClasses.addAll(collect);
  }

  private boolean checkForEvent(Class<?> parameterType)
  {
    return Event.class.isAssignableFrom(parameterType);
  }

  public void register(Object o, Class<?> clazz)
  {
    List<Class<?>> classesToHandle = new ArrayList<>();
    do
    {
      classesToHandle.add(clazz);
      clazz = clazz.getSuperclass();
    } while (clazz != null && !clazz.equals(Object.class));

    classesToHandle.forEach(aClass -> register(o, aClass.getDeclaredMethods()));
  }

  public void unregister(Object instance)
  {
    eventClasses.removeIf(eventMethod -> eventMethod.getInstance().equals(instance));
  }

}
