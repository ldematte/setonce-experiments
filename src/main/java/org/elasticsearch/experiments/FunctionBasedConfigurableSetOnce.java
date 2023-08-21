package org.elasticsearch.experiments;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

/**
 * A SetOnce variant which is configurable at construction time - using a Function.
 * @param <T>
 */
public final class FunctionBasedConfigurableSetOnce<T> {

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
            VALUE = l.findVarHandle(FunctionBasedConfigurableSetOnce.class, "value", Holder.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final Function<FunctionBasedConfigurableSetOnce<T>, T> getter;

    /**
     * Note: using a static function-method reference in place of a lambda has no distinguishable effects on
     * performance (but it makes code cleaner, IMO)
     */
    @SuppressWarnings("unchecked")
    private static <T> T GetVolatile(FunctionBasedConfigurableSetOnce<T> x) {
        var h = (Holder<T>) VALUE.getVolatile(x);
        if (h == null)
            return null;
        return h.innerValue;
    }

    private static <T> Function<FunctionBasedConfigurableSetOnce<T>, T> buildFunctionFromMode(int mode) {
        if (mode == 1) {
            return x -> {
                var h = (Holder<T>) VALUE.getAcquire(x);
                if (h == null)
                    return null;
                return h.innerValue;
            };
        } else if (mode == 2) {
            return x -> {
                var h = (Holder<T>) VALUE.getOpaque(x);
                if (h == null)
                    return null;
                return h.innerValue;
            };
        }
        return FunctionBasedConfigurableSetOnce::GetVolatile;
    }

    public FunctionBasedConfigurableSetOnce(int mode) {
        this.getter = buildFunctionFromMode(mode);
    }


    /**
     * Creates a new instance with the internal object set to the given object.
     * @see #set(Object)
     */
    public FunctionBasedConfigurableSetOnce(T obj) {
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
        return getter.apply(this);
    }
}
