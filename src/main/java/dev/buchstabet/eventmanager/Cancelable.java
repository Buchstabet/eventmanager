package dev.buchstabet.eventmanager;

public interface Cancelable extends Event {

  boolean isCanceled();

}
