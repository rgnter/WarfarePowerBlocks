package xyz.rgnt.revoken.common.providers.storage.data.codec.impl;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.revoken.common.providers.storage.data.AuxData;
import xyz.rgnt.revoken.common.providers.storage.data.codec.AuxCodec;
import xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec;
import xyz.rgnt.revoken.common.providers.storage.data.codec.impl.ex.CodecException;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecField;
import xyz.rgnt.revoken.common.util.ThrowingBiFunction;
import xyz.rgnt.revoken.common.util.ThrowingFunction;

import java.util.*;

public class CommonTransformer extends AuxCodec.Transformer {

    @Getter
    public final Map<Class<?>, ThrowingFunction<Object, Object, Exception>> encodeTransformers = new HashMap<>() {{
        put(Byte.class, Object::toString);
        put(byte.class, get(Byte.class));
        put(Short.class, Object::toString);
        put(short.class, get(Short.class));
        put(Integer.class, Object::toString);
        put(int.class, get(Integer.class));
        put(Long.class, Object::toString);
        put(long.class, get(Long.class));

        put(Float.class, Object::toString);
        put(float.class, get(Float.class));
        put(Double.class, Object::toString);
        put(double.class, get(Double.class));

        put(String.class, Object::toString);
        put(Boolean.class, Object::toString);
        put(boolean.class, get(Boolean.class));

        put(Component.class, (source) -> GsonComponentSerializer.gson().serialize((Component) source));

        put(ICodec.class, (source) -> {
            final var target = (ICodec) source;
            final var dataType = target.dataAdapterType();
            final AuxData data = dataType.getDataFunction().apply(null);
            data.encode(target);

            return dataType.isParsable() ? data.toString() : data;
        });
    }};

    @Getter
    public final Map<Class<?>, ThrowingBiFunction<Object, Object, Object, Exception>> decodeTransformers = new HashMap<>() {{

        put(List.class, (origin, source) -> {
            return source;
        });

        put(Map.class, (origin, source) -> {
            AuxData root = (AuxData) source;

            // todo: parameterized generic type of Map

            root.getKeys().forEach(key -> {

            });

            return origin;
        });

        put(Float.class, (origin, source) -> {
            try {
                return Float.parseFloat(source.toString());
            } catch (Exception x) {
                throw new Exception("Required float value but got '" + source.getClass().getName() + "'");
            }
        });
        put(float.class, get(Float.class));

        put(Double.class, (origin, source) -> {
            try {
                return Double.parseDouble(source.toString());
            } catch (Exception x) {
                throw new Exception("Required double value but got '" + source.getClass().getName() + "'");
            }
        });
        put(double.class, get(Double.class));

        put(Byte.class, (origin, source) -> {
            try {
                return Byte.parseByte(source.toString());
            } catch (Exception x) {
                throw new Exception("Required byte value but got '" + source.getClass().getName() + "'");
            }
        });
        put(byte.class, get(Byte.class));

        put(Short.class, (origin, source) -> {
            try {
                return Short.parseShort(source.toString());
            } catch (Exception x) {
                throw new Exception("Required short value but got '" + source.getClass().getName() + "'");
            }
        });
        put(short.class, get(Short.class));


        put(Integer.class, (origin, source) -> {
            try {
                return Integer.parseInt(source.toString());
            } catch (Exception x) {
                throw new Exception("Required integer value but got '" + source.getClass().getName() + "'");
            }
        });
        put(int.class, get(Integer.class));

        put(Long.class, (origin, source) -> {
            try {
                return Long.parseLong(source.toString());
            } catch (Exception x) {
                throw new Exception("Required long integer value but got '" + source.getClass().getName() + "'");
            }
        });
        put(long.class, get(Long.class));

        put(Boolean.class, (origin, source) -> {
            try {
                return Boolean.parseBoolean(source.toString());
            } catch (Exception x) {
                throw new Exception("Required boolean value but got '" + source.getClass().getName() + "'");
            }
        });
        put(boolean.class, get(Boolean.class));

        put(String.class, (origin, source) -> {
            return source;
        });


        put(Component.class, (origin, source) -> GsonComponentSerializer.gson().deserialize((String) source));

        put(ICodec.class, (origin, source) -> {
            final var target = (ICodec) origin;
            final var dataType = target.dataAdapterType();

            final AuxData data;
            try {
               data = dataType.getDataFunction().apply(source);
            } catch (ClassCastException x) {
                throw new Exception("Specified data adapter(" + dataType.getClass().getSimpleName() + ") failed to process data." , x);
            }
            data.decode(target);

            return target;
        });
    }};


    @Override
    public void encode(@NotNull CodecField codecField, @NotNull AuxData data) throws CodecException {
        final String key = codecField.getCodecKey().value();
        final Class<?> type = codecField.getCodecValue().getType();
        final Object value = codecField.getCodecValue().getValue();


        if (!this.encodeTransformers.containsKey(type))
            throw new CodecException("Couldn't find suitable encode transformer for type class " + type.getName(), codecField);

        final var transformer = this.encodeTransformers.get(type);
        try {
            data.set(key, transformer.apply(value));
        } catch (Exception e) {
            throw new CodecException("Failed to encode field", e, codecField);
        }
    }

    @Override
    public void decode(@NotNull CodecField codecField, @NotNull AuxData data) throws CodecException {
        final String key = codecField.getCodecKey().value();
        final Class<?> type;
        final Object origin = codecField.getCodecValue().getValue();

        final boolean isArray = List.class.isAssignableFrom(codecField.getCodecValue().getType());
        final boolean isMap   = Map.class.isAssignableFrom(codecField.getCodecValue().getType());

        if (ICodec.class.isAssignableFrom(codecField.getCodecValue().getType()))
            type = ICodec.class;
        else
            type = codecField.getCodecValue().getType();

        final var transformer = this.decodeTransformers.get(type);
        if (transformer == null)
            throw new CodecException("Couldn't find suitable decode transformer for class field", codecField);

        Object source;
        // if codec field is ICodec
        if (type.equals(ICodec.class)) {
            // require default value for codec field
            if (origin == null)
                throw new CodecException("Specify default value (transformer can not deduce class fields of specified codec)", codecField);

            // is source parsable or data
            if (!((ICodec) origin).dataAdapterType().isParsable())
                source = data.getSector(key);
            else {
                source = data.getString(key);
            }
        } else {
            if (isArray)
                source = data.getStringList(key);
            else if(isMap) {
                source = data.getSector(key);
            }
            else
                source = data.getString(key);
        }

        if (source == null) {
            if(origin == null)
                throw new CodecException("Missing codec key in data", codecField);
            else // use default value of field
                source = origin;
        }
        try {
            Object val = transformer.apply(origin, source);
            codecField.getCodecValue().setValue(val);
        } catch (Exception e) {
            throw new CodecException("Failed to decode field", e, codecField);
        }

    }


}
