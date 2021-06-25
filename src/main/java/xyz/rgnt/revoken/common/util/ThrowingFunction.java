package xyz.rgnt.revoken.common.util;

@FunctionalInterface
public interface ThrowingFunction<T, R, X extends Throwable> {

    abstract R apply(T value) throws X, Exception;

}
