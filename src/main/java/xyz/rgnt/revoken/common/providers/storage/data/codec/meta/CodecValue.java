package xyz.rgnt.revoken.common.providers.storage.data.codec.meta;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a codec value for any {@link xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec} class
 */
@Builder
public class CodecValue {

    @Getter
    private Class<?> type;
    @Getter @Setter
    private Object value;

}
