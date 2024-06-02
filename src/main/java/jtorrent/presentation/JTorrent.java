package jtorrent.presentation;

import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.stage.Stage;
import jtorrent.data.torrent.repository.AppPieceRepository;
import jtorrent.data.torrent.repository.AppTorrentMetadataRepository;
import jtorrent.data.torrent.repository.AppTorrentRepository;
import jtorrent.domain.Client;
import jtorrent.domain.common.Constants;
import jtorrent.domain.dht.DhtClient;
import jtorrent.domain.inbound.InboundConnectionListener;
import jtorrent.domain.lsd.LocalServiceDiscoveryManager;
import jtorrent.domain.torrent.repository.PieceRepository;
import jtorrent.domain.torrent.repository.TorrentMetadataRepository;
import jtorrent.domain.torrent.repository.TorrentRepository;
import jtorrent.presentation.main.viewmodel.MainViewModel;

public class JTorrent extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(JTorrent.class);

    private UiManager uiManager;
    private Client client;

    @Override
    public void init() throws Exception {
        ServerSocket serverSocket = new ServerSocket(Constants.PORT);
        InboundConnectionListener inboundConnectionListener = new InboundConnectionListener(serverSocket);

        DhtClient dhtClient = new DhtClient(Constants.PORT);

        TorrentRepository torrentRepository = new AppTorrentRepository();
        TorrentMetadataRepository torrentMetadataRepository = new AppTorrentMetadataRepository();
        PieceRepository pieceRepository = new AppPieceRepository();
        client = new Client(torrentRepository, torrentMetadataRepository, pieceRepository, inboundConnectionListener,
                new LocalServiceDiscoveryManager(), dhtClient);
    }

    @Override
    public void start(Stage primaryStage) {
        uiManager = new UiManager(primaryStage, new MainViewModel(client));
        uiManager.show();
    }

    @Override
    public void stop() {
        LOGGER.info("Stopping JTorrent");
        client.shutdown();
    }
}
