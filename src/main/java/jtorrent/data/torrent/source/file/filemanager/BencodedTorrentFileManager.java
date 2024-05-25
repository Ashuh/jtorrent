package jtorrent.data.torrent.source.file.filemanager;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import jtorrent.data.torrent.source.file.model.BencodedTorrent;

public class BencodedTorrentFileManager {

    /**
     * Reads a torrent file from the given URL.
     *
     * @param url the URL where the torrent file is located
     * @return the bencoded torrent read from the file
     * @throws IOException if an error occurs while reading the file
     */
    public BencodedTorrent read(URL url) throws IOException {
        // For some reason decoding directly from the URL stream doesn't work, so we have to read it into a byte array
        // first.
        try (BufferedInputStream in = new BufferedInputStream(url.openStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream();
        ) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }

            try (InputStream inputStream = new ByteArrayInputStream(out.toByteArray())) {
                return BencodedTorrent.decode(inputStream);
            }
        }
    }

    /**
     * Reads a torrent file from the given file.
     *
     * @param file the file to read the torrent from
     * @return the bencoded torrent read from the file
     * @throws IOException if an error occurs while reading the file
     */
    public BencodedTorrent read(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        return BencodedTorrent.decode(inputStream);
    }

    /**
     * Creates a new torrent file at the given path with the given bencoded torrent.
     *
     * @param path            the path to create the torrent file at
     * @param bencodedTorrent the bencoded torrent to write
     * @throws IOException if an error occurs while writing the file
     */
    public void write(Path path, BencodedTorrent bencodedTorrent) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            outputStream.write(bencodedTorrent.bencode());
        }
    }
}
