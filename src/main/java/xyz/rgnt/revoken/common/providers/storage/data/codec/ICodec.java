package xyz.rgnt.revoken.common.providers.storage.data.codec;


import xyz.rgnt.revoken.common.providers.storage.data.AuxData;

/**
 * Represents class which is able to be encoded/decoded by {@link AuxCodec}
 */
public interface ICodec {

    /**
     * @return Default type of this class
     */
    default Class<?> type() {
        return this.getClass();
    }

    /**
     * @return Default transformer for this codec
     */
    default AuxCodec.Transformer defaultTransformer() {
        return AuxCodec.COMMON_TRANSFORMER;
    }

    /**
     * @return Default class mapper for this codec
     */
    default AuxCodec.ClassMapper defaultClassMapper() {
        return AuxCodec.COMMON_CLASS_MAPPER;
    }

    /**
     * @return Default codec data adapter. Used only for decode operations.
     */
    default AuxData.TypeAdapter dataAdapterType() {
        return AuxData.TypeAdapter.YAML;
    }
}
