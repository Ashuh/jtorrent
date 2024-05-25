package jtorrent.data.torrent.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jtorrent.data.torrent.source.db.dao.TorrentDao;
import jtorrent.data.torrent.source.db.model.TorrentEntity;
import jtorrent.domain.common.util.Sha1Hash;
import jtorrent.domain.common.util.rx.MutableRxObservableList;
import jtorrent.domain.common.util.rx.RxObservableList;
import jtorrent.domain.torrent.model.Torrent;
import jtorrent.domain.torrent.repository.TorrentRepository;

public class AppTorrentRepository implements TorrentRepository {

    private final MutableRxObservableList<Torrent> torrentsObservable;
    private final Map<Sha1Hash, Torrent> infoHashToTorrent;
    private final TorrentDao torrentDao = new TorrentDao();

    public AppTorrentRepository() {
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

    private boolean isExistingTorrent(Torrent torrent) {
        return infoHashToTorrent.containsKey(torrent.getInfoHash());
    }
}
