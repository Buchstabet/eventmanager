package dev.buchstabet.eventmanager;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
        .filter(eventMethod -> isInstanceAndSameGeneric(eventMethod, v)).forEach(eventMethod ->
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

  private <V extends Event> boolean isInstanceAndSameGeneric(EventMethod eventMethod, V v)
  {
    Class<?> targetClass = eventMethod.getEvent();
    Type targetGeneric = v.getClass();

    if (targetClass.isInstance(v.getClass()))
    {
      Type genericType = targetClass.getGenericSuperclass();
      if (genericType instanceof ParameterizedType)
      {
        Type[] actualGenerics = ((ParameterizedType) genericType).getActualTypeArguments();
        for (Type t : actualGenerics)
        {
          if (t.equals(targetGeneric))
          {
            return true;
          }
        }
      }
    }

    return false;
  }


  public void register(Object o) throws EventException
  {
    register(o, o.getClass());
  }

  public void register(Object o, Class<?> clazz) throws EventException
  {
    List<EventMethod> collect = Arrays.stream(clazz.getDeclaredMethods())
        .filter(method -> method.getParameterTypes().length == 1).filter(
            method -> Arrays.asList(method.getParameterTypes()[0].getInterfaces())
                .contains(Event.class))
        .filter(method -> method.isAnnotationPresent(EventManager.class)).map(
            method -> new EventMethod(method,
                (Class<? extends Event>) method.getParameterTypes()[0], o))
        .collect(Collectors.toList());
    eventClasses.addAll(collect);
  }

  public void unregister(Object instance)
  {
    eventClasses.removeIf(eventMethod -> eventMethod.getInstance().equals(instance));
  }

}
