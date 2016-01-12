package org.jdbcdslog;

public interface HandlerCreator3<T, U, V, R> {
    R apply(T t, U u, V v);
}
