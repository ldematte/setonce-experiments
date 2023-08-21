package org.elasticsearch.experiments;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A SetOnce variant whose access semantics are configurable at construction time - using a Function.
 * @param <T>
 */
public final class ClassBasedConfigurableSetOnce<T> {

    private interface Getter<V> {
        V get();
    }

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
            VALUE = l.findVarHandle(ClassBasedConfigurableSetOnce.class, "value", Holder.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Getter<T> getter;

    /**
     * Note: using a class instead of an anonymous inner class does not change performance characteristics.
     * Capturing "this", instead, will. See FunctionBasedConfigurableSetOnce for comparison.
     */
    private static class VolatileGetter<V> implements Getter<V> {
        private final ClassBasedConfigurableSetOnce<V> x;

        private VolatileGetter(ClassBasedConfigurableSetOnce<V> x) {
            this.x = x;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V get() {
            var h = (Holder<V>) VALUE.getVolatile(x);
            if (h == null)
                return null;
            return h.innerValue;
        }
    }

    private Getter<T> buildFunctionFromMode(int mode) {
        if (mode == 1) {
            return () -> {
                var h = (Holder<T>) VALUE.getOpaque(this);
                if (h == null)
                    return null;
                return h.innerValue;
            };
        } else if (mode == 2) {
            return () -> {
                var h = (Holder<T>) VALUE.getOpaque(this);
                if (h == null)
                    return null;
                return h.innerValue;
            };
        }
        return new VolatileGetter<>(this);
    }

    public ClassBasedConfigurableSetOnce(int mode) {
        this.getter = buildFunctionFromMode(mode);
    }


    /**
     * Creates a new instance with the internal object set to the given object.
     * @see #set(Object)
     */
    public ClassBasedConfigurableSetOnce(T obj) {
        this.getter = buildFunctionFromMode(0);
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
    public T get() {
        return getter.get();
    }
}
