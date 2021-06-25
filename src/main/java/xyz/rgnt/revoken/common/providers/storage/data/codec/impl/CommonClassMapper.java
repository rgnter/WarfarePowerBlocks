package xyz.rgnt.revoken.common.providers.storage.data.codec.impl;


import org.jetbrains.annotations.NotNull;
import xyz.rgnt.revoken.common.providers.storage.data.codec.AuxCodec;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

public class CommonClassMapper extends AuxCodec.ClassMapper {

    @Override
    public @NotNull Stream<@NotNull Field> getClassCodecFields(@NotNull Class<?> clazz, @NotNull Object object) {
        return Arrays.stream(clazz.getDeclaredFields());
    }
}
