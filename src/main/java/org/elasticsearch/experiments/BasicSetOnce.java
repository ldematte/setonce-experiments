package org.elasticsearch.experiments;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * An implementation which is functionally and semantically equivalent to Lucene SetOnce.
 * It uses the same intermediate "holder" class to distinguish between "not set" and "set to null" use cases.
 * The only difference is that we are using VarHandles directly instead of using them through AtomicReference<> like
 * Lucene SetOnce does.
 * @param <T>
 */
public final class BasicSetOnce<T> {

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
            VALUE = l.findVarHandle(BasicSetOnce.class, "value", Holder.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * A default constructor which does not set the internal object, and allows setting it by calling
     * {@link #set(Object)}.
     */
    public BasicSetOnce() { }

    /**
     * Creates a new instance with the internal object set to the given object.
     * @see #set(Object)
     */
    public BasicSetOnce(T obj) {
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
        var h = ((Holder<T>)VALUE.getOpaque(this));
        if (h == null)
            return null;
        return h.innerValue;
    }
}
