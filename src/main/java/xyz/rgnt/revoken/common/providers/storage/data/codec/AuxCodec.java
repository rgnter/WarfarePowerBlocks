package xyz.rgnt.revoken.common.providers.storage.data.codec;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.revoken.common.providers.storage.data.AuxData;
import xyz.rgnt.revoken.common.providers.storage.data.codec.ex.ClassCodecException;
import xyz.rgnt.revoken.common.providers.storage.data.codec.impl.CommonClassMapper;
import xyz.rgnt.revoken.common.providers.storage.data.codec.impl.CommonTransformer;
import xyz.rgnt.revoken.common.providers.storage.data.codec.impl.ex.CodecException;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecField;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecKey;
import xyz.rgnt.revoken.common.providers.storage.data.codec.meta.CodecValue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides codec operations on classes.
 */
@Log4j2(topic = "AuxCodec")
public class AuxCodec {

    public static final ClassMapper COMMON_CLASS_MAPPER = new CommonClassMapper();
    public static final Transformer COMMON_TRANSFORMER = new CommonTransformer();

    /***
     * Encodes object
     * @param toEncode Object to encode
     * @param data     Data to which encoded class will be written.
     */
    public static void encodeClass(@NotNull ICodec toEncode, @NotNull AuxData data) throws ClassCodecException {
        final var transformer = toEncode.defaultTransformer();
        if(transformer == null)
            throw new ClassCodecException("Transformer is not present", toEncode);
        final var mapper = toEncode.defaultClassMapper();
        if(mapper == null)
            throw new ClassCodecException("ClassMapper is not present", toEncode);

        for (final Field field :
                mapper.getClassCodecFields(toEncode.type(), toEncode)
                        .filter(field -> !Modifier.isTransient(field.getModifiers()))
                        .filter(field -> field.isAnnotationPresent(CodecKey.class)).collect(Collectors.toList())) {
            field.setAccessible(true);

            CodecKey cKey = field.getDeclaredAnnotation(CodecKey.class);
            if (cKey == null)
                return;

            try {
                CodecValue cVal = CodecValue.builder()
                        .type(field.getType())
                        .value(field.get(toEncode))
                        .build();
                CodecField cField = CodecField.builder()
                        .fieldName(field.getName())
                        .codecKey(cKey)
                        .codecValue(cVal)
                        .build();

                transformer.encode(cField, data);
            } catch (Exception e) {
                throw new ClassCodecException(toEncode, e);
            }
        }

        // user defined encode
        try {
            toEncode.onEncode(data);
        } catch (Exception x) {
            throw new ClassCodecException(toEncode, x);
        }
    }

    /***
     * Decodes object
     * @param toDecode Object to decode. (This will modify the object codec members)
     * @param data     Data from which decoded class will be read.
     */
    public static void decode(@NotNull ICodec toDecode, @NotNull AuxData data) throws ClassCodecException {
        final var transformer = toDecode.defaultTransformer();
        if(transformer == null)
            throw new ClassCodecException("Transformer is not present", toDecode);
        final var mapper = toDecode.defaultClassMapper();
        if(mapper == null)
            throw new ClassCodecException("ClassMapper is not present", toDecode);

        for (final Field field :
                mapper.getClassCodecFields(toDecode.type(), toDecode)
                        .filter(field -> !Modifier.isTransient(field.getModifiers()))
                        .filter(field -> field.isAnnotationPresent(CodecKey.class)).collect(Collectors.toList())) {
            field.setAccessible(true);

            CodecKey cKey = field.getDeclaredAnnotation(CodecKey.class);
            if (cKey == null)
                return;

            try {
                CodecValue cVal = CodecValue.builder()
                        .type(field.getType())
                        .value(field.get(toDecode))
                        .build();
                CodecField cField = CodecField.builder()
                        .fieldName(field.getName())
                        .codecKey(cKey)
                        .codecValue(cVal)
                        .build();
                transformer.decode(cField, data);

                field.set(toDecode, cField.getCodecValue().getValue());
            } catch (Exception e) {
                throw new ClassCodecException(toDecode, e);
            }
        }

        // user defined decode
        try {
            toDecode.onDecode(data);
        } catch (Exception x) {
            throw new ClassCodecException(toDecode, x);
        }
    }

    /**
     * ClassMapper handles field and key mapping
     */
    public static abstract class ClassMapper {
        public abstract @NotNull Stream<@NotNull Field> getClassCodecFields(@NotNull Class<?> clazz, @NotNull Object object);
    }

    /**
     * Transformer handles transformation between data and fields
     */
    public static abstract class Transformer {
        public abstract void encode(@NotNull CodecField codecField, @NotNull AuxData data) throws CodecException;

        public abstract void decode(@NotNull CodecField codecField, @NotNull AuxData data) throws CodecException;
    }

}
