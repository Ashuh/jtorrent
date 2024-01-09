package jtorrent.domain.common.util.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.dampcake.bencode.BencodeOutputStream;

public abstract class BencodedObject {

    public byte[] bencode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BencodeOutputStream bos = new BencodeOutputStream(baos, StandardCharsets.UTF_8);
        try {
            bos.writeDictionary(toMap());
        } catch (IOException e) {
            // This should never happen since IOException is only thrown if the underlying stream is closed.
            throw new AssertionError(e);
        }
        return baos.toByteArray();
    }

    public abstract Map<String, Object> toMap();
}
