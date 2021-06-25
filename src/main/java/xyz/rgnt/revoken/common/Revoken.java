package xyz.rgnt.revoken.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;

public interface Revoken<T> {

    public T instance();

    @Nullable InputStream getResource(@NotNull String resourcePath);

    @NotNull File getDataFolder();
}
