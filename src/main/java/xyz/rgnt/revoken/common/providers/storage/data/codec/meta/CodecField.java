package xyz.rgnt.revoken.common.providers.storage.data.codec.meta;

import lombok.Builder;
import lombok.Getter;

/**
 * Represents a member field from any {@link xyz.rgnt.revoken.common.providers.storage.data.codec.ICodec} class
 */
@Builder @Getter
public class CodecField {

    private final String fieldName;
    private final CodecKey codecKey;
    private final CodecValue codecValue;

}
