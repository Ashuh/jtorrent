package jtorrent.domain.torrent.model;

import static jtorrent.domain.common.util.ValidationUtil.requireNonNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.rx.CombinedDoubleSumObservable;
import jtorrent.domain.common.util.rx.MutableRxObservableSet;
import jtorrent.domain.common.util.rx.RxObservableSet;
import jtorrent.domain.peer.model.Peer;
import jtorrent.domain.peer.model.PeerContactInfo;
import jtorrent.domain.tracker.handler.TrackerHandler;
import jtorrent.domain.tracker.model.Tracker;
import jtorrent.domain.tracker.model.factory.TrackerFactory;

public class Torrent implements TrackerHandler.TorrentProgressProvider {

    private final TorrentMetadata torrentMetaData;
    private final TorrentStatistics torrentStatistics = new TorrentStatistics();
    private final TorrentProgress torrentProgress;
    private final Set<Tracker> trackers = new HashSet<>();
    private final MutableRxObservableSet<Peer> peers = new MutableRxObservableSet<>(new HashSet<>());
    private final CombinedDoubleSumObservable downloadRateObservable = new CombinedDoubleSumObservable();
    private final CombinedDoubleSumObservable uploadRateObservable = new CombinedDoubleSumObservable();
    private final BehaviorSubject<String> nameSubject = BehaviorSubject.createDefault("");
    private String name;
    private Path saveDirectory;

    private State state = State.STOPPED;
    private final BehaviorSubject<State> stateSubject = BehaviorSubject.createDefault(state);

    public Torrent(TorrentMetadata torrentMetaData) {
        // TODO: use default downloads folder?
        this(torrentMetaData, torrentMetaData.fileInfo().getName(), Paths.get("download").toAbsolutePath());
    }

    public Torrent(TorrentMetadata torrentMetaData, String name, Path saveDirectory) {
        this.torrentMetaData = requireNonNull(torrentMetaData);
        this.torrentProgress = new TorrentProgress(torrentMetaData.fileInfo());
        this.name = name;
        this.saveDirectory = saveDirectory;
        torrentMetaData.trackers().stream()
                .map(TrackerFactory::fromUri)
                .collect(Collectors.toCollection(() -> trackers));
    }

    public Path getSaveDirectory() {
        return saveDirectory;
    }

    public void setSaveDirectory(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
    }

    /**
     * Gets the root save directory for this torrent.
     * The paths of the files in the torrent are relative to the root save directory.
     * If the torrent is a single-file torrent, the root save directory is the save directory.
     * If the torrent is a multi-file torrent, the root save directory is a subdirectory of the save directory.
     *
     * @return the root save directory for this torrent
     */
    public Path getRootSaveDirectory() {
        return saveDirectory.resolve(torrentMetaData.fileInfo().getFileRoot());
    }

    /**
     * Gets the path to the file or directory to which the torrent contents should be saved.
     * If the torrent is a single file torrent, then the returned path is the path to the file.
     * If the torrent is a multi-file torrent, then the returned path is the path to the directory containing the files.
     */
    public Path getSaveAsPath() {
        return saveDirectory.resolve(torrentMetaData.fileInfo().getName());
    }

    public Set<Tracker> getTrackers() {
        return trackers;
    }

    public LocalDateTime getCreationDate() {
        return torrentMetaData.creationDate();
    }

    public String getComment() {
        return torrentMetaData.comment();
    }

    public String getCreatedBy() {
        return torrentMetaData.createdBy();
    }

    public int getPieceSize() {
        return torrentMetaData.fileInfo().getPieceSize();
    }

    public int getPieceSize(int pieceIndex) {
        return torrentMetaData.fileInfo().getPieceSize(pieceIndex);
    }

    public int getBlockSize() {
        return torrentMetaData.fileInfo().getBlockSize();
    }

    public int getBlockSize(int pieceIndex, int blockIndex) {
        return torrentMetaData.fileInfo().getBlockSize(pieceIndex, blockIndex);
    }

    public Sha1Hash getPieceHash(int piece) {
        return torrentMetaData.fileInfo().getPieceHash(piece);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<FileMetadata> getFileMetadataInRange(long start, long end) {
        return torrentMetaData.fileInfo().getInRange(start, end);
    }

    public List<FileMetadataWithState> getFileMetaDataWithState() {
        return torrentMetaData.fileInfo().getFileMetaData()
                .stream()
                .map(fileMetaData -> new FileMetadataWithState(fileMetaData,
                        torrentProgress.getFileState(fileMetaData.path())))
                .toList();
    }

    @Override
    public Sha1Hash getInfoHash() {
        return torrentMetaData.infoHash();
    }

    @Override
    public long getDownloaded() {
        return torrentStatistics.getDownloaded();
    }

    @Override
    public long getLeft() {
        return getTotalSize() - getVerifiedBytes();
    }

    @Override
    public long getUploaded() {
        return torrentStatistics.getUploaded();
    }

    public long getTotalSize() {
        return torrentMetaData.fileInfo().getTotalFileSize();
    }

    private long getVerifiedBytes() {
        return torrentProgress.getVerifiedBytes();
    }

    public int getNumPieces() {
        return torrentMetaData.fileInfo().getNumPieces();
    }

    public long getPieceOffset(int index) {
        return torrentMetaData.fileInfo().getPieceOffset(index);
    }

    public void incrementDownloaded(int amount) {
        torrentStatistics.incrementDownloaded(amount);
    }

    public void incrementUploaded(int amount) {
        torrentStatistics.incrementUploaded(amount);
    }

    public void setBlockReceived(int pieceIndex, int blockIndex) {
        torrentProgress.setBlockReceived(pieceIndex, blockIndex);
    }

    public void setBlockNotRequested(int pieceIndex, int blockIndex) {
        torrentProgress.setBlockNotRequested(pieceIndex, blockIndex);
    }

    public void setPieceMissing(int piece) {
        torrentProgress.setPieceMissing(piece);
    }

    public BitSet getCompletelyMissingPiecesWithUnrequestedBlocks() {
        return torrentProgress.getCompletelyMissingPiecesWithUnrequestedBlocks();
    }

    public BitSet getPartiallyMissingPiecesWithUnrequestedBlocks() {
        return torrentProgress.getPartiallyMissingPiecesWithUnrequestedBlocks();
    }

    public BitSet getVerifiedPieces() {
        return torrentProgress.getVerifiedPieces();
    }

    public Observable<BitSet> getVerifiedPiecesObservable() {
        return torrentProgress.getVerifiedPiecesObservable();
    }

    public Observable<BitSet> getAvailablePiecesObservable() {
        return torrentProgress.getAvailablePiecesObservable();
    }

    public void setPieceVerified(int piece) {
        torrentProgress.setPieceVerified(piece);
    }

    public void setPieceChecked(int pieceIndex) {
        torrentProgress.setPieceChecked(pieceIndex);
    }

    public void resetCheckedBytes() {
        torrentProgress.resetCheckedBytes();
    }

    public boolean isPieceComplete(int pieceIndex) {
        return torrentProgress.isPieceComplete(pieceIndex);
    }

    public boolean isAllPiecesVerified() {
        return torrentProgress.isAllPiecesVerified();
    }

    public BitSet getMissingBlocks(int pieceIndex) {
        return torrentProgress.getMissingBlocks(pieceIndex);
    }

    public void setBlockRequested(int pieceIndex, int blockIndex) {
        torrentProgress.setBlockRequested(pieceIndex, blockIndex);
    }

    public double getDownloadRate() {
        return peers.getCollection().stream()
                .mapToDouble(Peer::getDownloadRate)
                .sum();
    }

    public Observable<Double> getDownloadRateObservable() {
        return downloadRateObservable;
    }

    public Observable<Integer> getDownloadedObservable() {
        return torrentStatistics.getDownloadedObservable();
    }

    public double getUploadRate() {
        return peers.getCollection().stream()
                .mapToDouble(Peer::getUploadRate)
                .sum();
    }

    public Observable<Double> getUploadRateObservable() {
        return uploadRateObservable;
    }

    public Observable<Integer> getUploadedObservable() {
        return torrentStatistics.getUploadedObservable();
    }

    public Observable<Long> getVerifiedBytesObservable() {
        return torrentProgress.getVerifiedBytesObservable();
    }

    public Observable<Long> getCheckedBytesObservable() {
        return torrentProgress.getCheckedBytesObservable();
    }

    public RxObservableSet<Peer> getPeersObservable() {
        return peers;
    }

    public void addPeer(Peer peer) {
        peers.add(peer);
        downloadRateObservable.addSource(peer.getDownloadRateObservable());
        uploadRateObservable.addSource(peer.getUploadRateObservable());
    }

    public void removePeer(Peer peer) {
        peers.remove(peer);
        downloadRateObservable.removeSource(peer.getDownloadRateObservable());
        uploadRateObservable.removeSource(peer.getUploadRateObservable());
    }

    public void clearPeers() {
        peers.clear();
        downloadRateObservable.clearSources();
        uploadRateObservable.clearSources();
    }

    public boolean hasPeer(PeerContactInfo peerContactInfo) {
        return peers.anyMatch(peer -> peer.getPeerContactInfo().equals(peerContactInfo));
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        stateSubject.onNext(state);
    }

    public Observable<State> getStateObservable() {
        return stateSubject;
    }

    @Override
    public String toString() {
        return "Torrent{"
                + "trackers=" + trackers
                + ", name='" + name + '\''
                + '}';
    }

    public enum State {
        STOPPED,
        CHECKING,
        DOWNLOADING,
        SEEDING
    }

}
