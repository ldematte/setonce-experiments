package org.elasticsearch.experiments;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A SetOnce variant where the held object and the "set" flag are separate. Notice that in this case some stronger
 * models are required (Volatile, or at least Release/Acquire).
 * @param <T>
 */
public final class SeparateFlagSetOnce<T> {

    private T value;
    private static final VarHandle VALUE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(SeparateFlagSetOnce.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final AtomicBoolean isSet;

    /**
     * A default constructor which does not set the internal object, and allows setting it by calling
     * {@link #set(Object)}.
     */
    public SeparateFlagSetOnce() {
        isSet = new AtomicBoolean(false);
    }

    /**
     * Creates a new instance with the internal object set to the given object.
     * @see #set(Object)
     */
    public SeparateFlagSetOnce(T obj) {
        isSet = new AtomicBoolean(true);
        VALUE.setVolatile(this, obj);
    }

    /** Sets the given object. If the object has already been set, an exception is thrown. */
    public void set(T obj) {
        if (!trySet(obj)) {
            throw new RuntimeException();
        }
    }

    /**
     * Sets the given object if none was set before.
     *
     * @return true if object was set successfully, false otherwise
     */
    public boolean trySet(T obj) {
        boolean alreadySet = isSet.compareAndSet(false, true);
        if (alreadySet == false) {
            VALUE.setVolatile(this, obj);
        }
        return alreadySet;
    }

    /** Returns the object set by set. */
    @SuppressWarnings("unchecked")
    public T get() {
        return (T)VALUE.getVolatile(this);
    }
}
