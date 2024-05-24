package jtorrent.data.torrent.repository;

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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jtorrent.data.torrent.source.db.dao.TorrentDao;
import jtorrent.data.torrent.source.db.model.TorrentEntity;
import jtorrent.data.torrent.source.file.model.BencodedFile;
import jtorrent.data.torrent.source.file.model.BencodedInfo;
import jtorrent.data.torrent.source.file.model.BencodedMultiFileInfo;
import jtorrent.data.torrent.source.file.model.BencodedSingleFileInfo;
import jtorrent.data.torrent.source.file.model.BencodedTorrent;
import jtorrent.domain.common.util.ContinuousMergedInputStream;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.rx.MutableRxObservableList;
import jtorrent.domain.common.util.rx.RxObservableList;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.domain.torrent.repository.TorrentRepository;

public class FileTorrentRepository implements TorrentRepository {

    private final MutableRxObservableList<Torrent> torrentsObservable;
    private final Map<Sha1Hash, Torrent> infoHashToTorrent;
    private final TorrentDao torrentDao = new TorrentDao();

    public FileTorrentRepository() {
        List<Torrent> torrents = new ArrayList<>();
        torrentDao.readAll().stream()
                .map(TorrentEntity::toDomain)
                .forEach(torrents::add);
        infoHashToTorrent = torrents.stream()
                .collect(HashMap::new, (map, torrent) -> map.put(torrent.getInfoHash(), torrent), Map::putAll);
        this.torrentsObservable = new MutableRxObservableList<>(torrents);
    }

    @Override
    public RxObservableList<Torrent> getTorrents() {
        return torrentsObservable;
    }

    @Override
    public Torrent getTorrent(Sha1Hash infoHash) {
        return infoHashToTorrent.get(infoHash);
    }

    @Override
    public void addTorrent(Torrent torrent) {
        if (isExistingTorrent(torrent)) {
            // TODO: maybe throw exception if torrent already exists?
            return;
        }
        torrentDao.create(TorrentEntity.fromDomain(torrent));
        infoHashToTorrent.put(torrent.getInfoHash(), torrent);
        torrentsObservable.add(torrent);
    }

    @Override
    public void persistTorrents() {
        infoHashToTorrent.values().stream()
                .map(TorrentEntity::fromDomain)
                .forEach(torrentDao::update);
    }

    @Override
    public void removeTorrent(Torrent torrent) {
        torrentDao.delete(torrent.getInfoHash().getBytes());
        infoHashToTorrent.remove(torrent.getInfoHash());
        torrentsObservable.remove(torrent);
    }

    @Override
    public TorrentMetadata loadTorrent(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        return loadTorrent(inputStream);
    }

    private TorrentMetadata loadTorrent(InputStream inputStream) throws IOException {
        return BencodedTorrent.decode(inputStream).toDomain();
    }

    @Override
    public TorrentMetadata loadTorrent(URL url) throws IOException {
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
                return loadTorrent(inputStream);
            }
        }
    }

    @Override
    public void saveTorrent(TorrentMetadata torrentMetadata, Path savePath) throws IOException {
        BencodedTorrent bencodedTorrent = BencodedTorrent.fromDomain(torrentMetadata);
        try (OutputStream outputStream = Files.newOutputStream(savePath)) {
            outputStream.write(bencodedTorrent.bencode());
        }
    }

    /**
     * Create a new {@link TorrentMetadata} instance with the current time as the creation date.
     *
     * @param trackerUrls list of tiers, each containing a list of tracker URLs
     * @param comment     comment about the torrent
     * @param createdBy   name and version of the program used to create the .torrent
     * @param pieceSize   size of each piece in bytes
     * @return a new {@link TorrentMetadata} instance
     */
    @Override
    public TorrentMetadata createNewTorrent(Path source, List<List<String>> trackerUrls, String comment,
            String createdBy, int pieceSize) throws IOException {
        Long creationDate = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        BencodedInfo info = buildBencodedInfo(source, pieceSize);
        return BencodedTorrent.withAnnounceList(creationDate, trackerUrls, comment, createdBy, info).toDomain();
    }

    private static BencodedInfo buildBencodedInfo(Path source, int pieceSize) throws IOException {
        if (Files.isDirectory(source)) {
            return buildBencodedMultiFileInfo(source, pieceSize);
        } else {
            return buildBencondedSingleFileInfo(source, pieceSize);
        }
    }

    private static BencodedSingleFileInfo buildBencondedSingleFileInfo(Path source, int pieceSize) throws IOException {
        if (Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must be a file");
        }

        byte[] hashes = computeHashes(Files.newInputStream(source), pieceSize);
        String fileName = source.getFileName().toString();
        long length = Files.size(source);
        return new BencodedSingleFileInfo(pieceSize, hashes, fileName, length);
    }

    private static BencodedMultiFileInfo buildBencodedMultiFileInfo(Path source, int pieceSize) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must be a directory");
        }

        List<Path> filePaths = getFilesInDirectory(source);

        List<InputStream> inputStreams = new ArrayList<>();
        List<BencodedFile> files = new ArrayList<>();
        for (Path filePath : filePaths) {
            long length = Files.size(filePath);
            files.add(BencodedFile.fromPath(source.relativize(filePath), length));
            inputStreams.add(Files.newInputStream(filePath));
        }

        byte[] hashes = computeHashes(new ContinuousMergedInputStream(inputStreams), pieceSize);
        String dirName = source.getFileName().toString();
        return new BencodedMultiFileInfo(pieceSize, hashes, dirName, files);
    }

    private static List<Path> getFilesInDirectory(Path directory) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    private static byte[] computeHashes(InputStream inputStream, int pieceSize) throws IOException {
        List<Sha1Hash> hashes = new ArrayList<>();
        int bytesRead;
        byte[] buffer = new byte[pieceSize];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byte[] piece = Arrays.copyOf(buffer, bytesRead);
            hashes.add(Sha1Hash.of(piece));
        }
        return Sha1Hash.concatHashes(hashes);
    }

    private boolean isExistingTorrent(Torrent torrent) {
        return infoHashToTorrent.containsKey(torrent.getInfoHash());
    }
}
