package jtorrent.data.torrent.model;

import static jtorrent.data.torrent.model.BencodedInfo.KEY_FILES;
import static jtorrent.data.torrent.model.BencodedInfo.KEY_LENGTH;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class BencodedInfoFactory {

    private BencodedInfoFactory() {
    }

    public static BencodedInfo fromMap(Map<String, Object> map) {
        if (map.containsKey(KEY_LENGTH)) {
            return BencodedSingleFileInfo.fromMap(map);
        } else if (map.containsKey(KEY_FILES)) {
            return BencodedMultiFileInfo.fromMap(map);
        }

        throw new IllegalArgumentException("Invalid info dictionary");
    }

    public static BencodedInfo fromPath(Path source, int pieceSize) throws IOException {
        if (Files.isDirectory(source)) {
            return BencodedMultiFileInfo.fromPath(source, pieceSize);
        } else {
            return BencodedSingleFileInfo.fromPath(source, pieceSize);
        }
    }
}
