package jtorrent.domain.repository;

import jtorrent.domain.model.torrent.Torrent;


public interface PieceRepository {

    byte[] getPiece(Torrent torrent, int index);

    void storeBlock(Torrent torrent, int index, int offset, byte[] data);
}
