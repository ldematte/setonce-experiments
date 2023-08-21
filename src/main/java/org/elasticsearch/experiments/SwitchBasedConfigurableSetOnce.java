package org.elasticsearch.experiments;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A SetOnce alternative where the variable access semantics are configurable - and evaluated each time during value
 * get. While this is wasteful (access mode cannot be changed after construction), it might have a performance
 * advantage (lambda invocation VS. JIT optimization because we always hit the same switch branch).
 * @param <T>
 */
public final class SwitchBasedConfigurableSetOnce<T> {

    private static class Holder<V> {
        final V innerValue;

        private Holder(V value) {
            this.innerValue = value;
        }
    }
    private Holder<T> value;
    private static final VarHandle VALUE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(SwitchBasedConfigurableSetOnce.class, "value", Holder.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    private final int mode;

    /**
     * A default constructor which does not set the internal object, and allows setting it by calling
     * {@link #set(Object)}.
     */
    public SwitchBasedConfigurableSetOnce(int mode) {
        this.mode = mode;
    }

    /**
     * Creates a new instance with the internal object set to the given object.
     * @see #set(Object)
     */
    public SwitchBasedConfigurableSetOnce(T obj) {
        this.mode = 0;
        VALUE.setVolatile(this, new Holder<>(obj));
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
        return VALUE.compareAndSet(this, null, new Holder<>(obj));
    }

    /** Returns the object set by set. */
    @SuppressWarnings("unchecked")
    public T get() {
        Holder<T> h;
        switch (mode)
        {
            case 1:
                h = (Holder<T>)VALUE.getOpaque(this);
            case 2:
                h = (Holder<T>)VALUE.getAcquire(this);
            default:
                h = (Holder<T>)VALUE.getVolatile(this);
        }

        if (h == null)
            return null;
        return h.innerValue;
    }
}
