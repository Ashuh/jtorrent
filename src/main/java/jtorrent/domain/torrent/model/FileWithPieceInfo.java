package jtorrent.domain.torrent.model;

import java.util.Objects;

public record FileWithPieceInfo(File file, FilePieceInfo filePieceInfo) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileWithPieceInfo that = (FileWithPieceInfo) o;
        return Objects.equals(file, that.file) && Objects.equals(filePieceInfo, that.filePieceInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, filePieceInfo);
    }
}
