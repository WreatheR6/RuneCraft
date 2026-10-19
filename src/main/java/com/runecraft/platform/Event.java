package com.runecraft.platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * A listener list with the shape of Fabric API's {@code Event}: {@code register(listener)} adds a listener and
 * {@code invoker()} returns one listener that calls all of them. The NeoForge bridges ({@link PlatformEvents},
 * {@code client.ClientPlatformEvents}) fire the invokers from NeoForge's events, so the mod's hook code keeps the
 * calls it was written with on Fabric.
 */
public final class Event<T> {
    private final List<T> listeners = new CopyOnWriteArrayList<>();
    private final Function<List<T>, T> combine;
    private final T invoker;

    public Event(Function<List<T>, T> combine) {
        this.combine = combine;
        this.invoker = combine.apply(listeners);
    }

    public void register(T listener) {
        listeners.add(listener);
    }

    /** One listener that calls every registered one (it reads the live list, so later registrations count too). */
    public T invoker() {
        return invoker;
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    Function<List<T>, T> combiner() {
        return combine;
    }
}
