package jtorrent.torrent.data.model;

import static jtorrent.torrent.data.model.BencodedInfo.KEY_FILES;
import static jtorrent.torrent.data.model.BencodedInfo.KEY_LENGTH;

import java.util.Map;

public class BencodedInfoFactory {

    private BencodedInfoFactory() {
    }

    public static BencodedInfo fromMap(Map<String, Object> map) {
        if (map.containsKey(KEY_LENGTH)) {
            return SingleFileInfo.fromMap(map);
        } else if (map.containsKey(KEY_FILES)) {
            return MultiFileInfo.fromMap(map);
        }

        throw new IllegalArgumentException("Invalid info dictionary");
    }
}
