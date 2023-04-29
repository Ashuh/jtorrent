package jtorrent.data.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import com.dampcake.bencode.BencodeOutputStream;

public abstract class BencodedObject {

    public byte[] bencode() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BencodeOutputStream bos = new BencodeOutputStream(baos);
        bos.writeDictionary(toMap());
        return baos.toByteArray();
    }

    public abstract Map<String, Object> toMap();
}
