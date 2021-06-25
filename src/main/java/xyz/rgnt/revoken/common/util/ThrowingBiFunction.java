package xyz.rgnt.revoken.common.util;

@FunctionalInterface
public interface ThrowingBiFunction<T, Y, R, X extends Throwable> {

    abstract R apply(T var0, Y var1) throws X, Exception;

}
