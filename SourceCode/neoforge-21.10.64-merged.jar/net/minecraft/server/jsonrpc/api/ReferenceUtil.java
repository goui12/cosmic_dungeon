package net.minecraft.server.jsonrpc.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.net.URI;
import java.net.URISyntaxException;

public class ReferenceUtil {
    public static final Codec<URI> REFERENCE_CODEC = Codec.STRING.comapFlatMap(p_451105_ -> {
        try {
            return DataResult.success(new URI(p_451105_));
        } catch (URISyntaxException urisyntaxexception) {
            return DataResult.error(urisyntaxexception::getMessage);
        }
    }, URI::toString);

    public static URI createLocalReference(String name) {
        return URI.create("#/components/schemas/" + name);
    }
}
