package org.jdbcdslog;

public interface HandlerCreator<T, R> {
    R apply(T t);
}
