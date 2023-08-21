package org.elasticsearch.experiments;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A SetOnce variant where the held object cannot be null. This allows us to get rid of the Holder class/of the
 * additional flag. It still follows the same Volatile semantics.
 * @param <T>
 */
public final class NonNullableSetOnce<T> {

    private T value;
    private static final VarHandle VALUE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(NonNullableSetOnce.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * A default constructor which does not set the internal object, and allows setting it by calling
     * {@link #set(Object)}.
     */
    public NonNullableSetOnce() { }

    /**
     * Creates a new instance with the internal object set to the given object.
     * @see #set(Object)
     */
    public NonNullableSetOnce(T obj) {
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
        return VALUE.compareAndSet(this, null, obj);
    }

    /** Returns the object set by set. */
    @SuppressWarnings("unchecked")
    public T get() {
        return (T)VALUE.getVolatile(this);
    }
}
