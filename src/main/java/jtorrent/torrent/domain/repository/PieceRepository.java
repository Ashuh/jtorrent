package jtorrent.torrent.domain.repository;

import jtorrent.torrent.domain.model.Torrent;


public interface PieceRepository {

    byte[] getPiece(Torrent torrent, int index);

    void storeBlock(Torrent torrent, int index, int offset, byte[] data);
}
