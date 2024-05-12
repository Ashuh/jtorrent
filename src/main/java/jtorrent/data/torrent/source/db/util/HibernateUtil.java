package jtorrent.data.torrent.source.db.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import jtorrent.data.torrent.source.db.model.TorrentEntity;

public class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            StandardServiceRegistry registry = new StandardServiceRegistryBuilder().build();
            sessionFactory = new MetadataSources(registry)
                    .addAnnotatedClass(TorrentEntity.class)
                    .buildMetadata()
                    .buildSessionFactory();
        }
        return sessionFactory;
    }
}
