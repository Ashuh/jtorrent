package jtorrent.data.torrent.source.file.model;

import static jtorrent.data.torrent.source.file.model.BencodedInfo.KEY_FILES;
import static jtorrent.data.torrent.source.file.model.BencodedInfo.KEY_LENGTH;

import java.util.Map;

import jtorrent.domain.torrent.model.FileInfo;
import jtorrent.domain.torrent.model.MultiFileInfo;
import jtorrent.domain.torrent.model.SingleFileInfo;

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

    public static BencodedInfo fromDomain(FileInfo fileInfo) {
        if (fileInfo instanceof SingleFileInfo singleFileInfo) {
            return BencodedSingleFileInfo.fromDomain(singleFileInfo);
        } else {
            return BencodedMultiFileInfo.fromDomain((MultiFileInfo) fileInfo);
        }
    }
}
