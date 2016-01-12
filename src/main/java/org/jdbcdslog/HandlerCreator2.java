package org.jdbcdslog;

public interface HandlerCreator2<T, U, R> {
    R apply(T t, U u);
}
