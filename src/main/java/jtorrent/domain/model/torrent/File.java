package jtorrent.domain.model.torrent;

import java.nio.file.Path;
import java.util.Objects;

public class File {

    private final int size;
    private final Path path;

    public File(int size, Path path) {
        this.size = size;
        this.path = path;
    }

    public int getSize() {
        return size;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        File file = (File) o;
        return size == file.size && path.equals(file.path);
    }

    @Override
    public String toString() {
        return "File{"
                + "size=" + size
                + ", path=" + path
                + '}';
    }
}
