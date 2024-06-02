package jtorrent.data.torrent.source.db.dao;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import jtorrent.data.torrent.source.db.model.TorrentEntity;
import jtorrent.data.torrent.source.db.util.HibernateUtil;

public class TorrentDao {

    private final SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    public void create(TorrentEntity torrentEntity) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(torrentEntity);
            session.getTransaction().commit();
        }
    }

    public List<TorrentEntity> readAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from TorrentEntity", TorrentEntity.class).list();
        }
    }

    public TorrentEntity read(byte[] infoHash) {
        try (Session session = sessionFactory.openSession()) {
            TorrentEntity torrentEntity = session.get(TorrentEntity.class, infoHash);
            if (torrentEntity == null) {
                throw new IllegalArgumentException(
                        "TorrentEntity with infoHash " + Arrays.toString(infoHash) + " does not exist");
            }
            return torrentEntity;
        }
    }

    public void update(TorrentEntity torrentEntity) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.merge(torrentEntity);
            session.getTransaction().commit();
        }
    }

    public void delete(byte[] infoHash) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            var entity = session.get(TorrentEntity.class, infoHash);
            session.remove(entity);
            session.getTransaction().commit();
        }
    }
}
