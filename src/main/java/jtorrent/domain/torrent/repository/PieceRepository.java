package jtorrent.domain.torrent.repository;

import java.io.IOException;

import jtorrent.domain.torrent.model.Torrent;


public interface PieceRepository {

    byte[] getPiece(Torrent torrent, int index) throws IOException;

    /**
     * Retrieves a block of data from a torrent.
     *
     * @param torrent the torrent from which to retrieve the block
     * @param index   the index of the piece containing the block
     * @param offset  the offset within the piece where the block starts
     * @param length  the length of the block to retrieve
     * @return the block of data as a byte array
     */
    byte[] getBlock(Torrent torrent, int index, int offset, int length) throws IOException;

    /**
     * Stores a block of data in a torrent.
     *
     * @param torrent the torrent in which to store the block
     * @param index   the index of the piece where the block should be stored
     * @param offset  the offset within the piece where the block should be stored
     * @param data    the block of data to be stored as a byte array
     */
    void storeBlock(Torrent torrent, int index, int offset, byte[] data) throws IOException;
}
