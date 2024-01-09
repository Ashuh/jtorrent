package jtorrent.presentation;

import java.lang.System.Logger.Level;
import java.net.ServerSocket;

import javafx.application.Application;
import javafx.stage.Stage;
import jtorrent.data.torrent.repository.FilePieceRepository;
import jtorrent.data.torrent.repository.FileTorrentRepository;
import jtorrent.domain.Client;
import jtorrent.domain.common.Constants;
import jtorrent.domain.dht.DhtClient;
import jtorrent.domain.inbound.InboundConnectionListener;
import jtorrent.domain.lsd.LocalServiceDiscoveryManager;
import jtorrent.domain.torrent.repository.PieceRepository;
import jtorrent.domain.torrent.repository.TorrentRepository;
import jtorrent.presentation.viewmodel.ViewModel;

public class JTorrent extends Application {

    private static final System.Logger LOGGER = System.getLogger(JTorrent.class.getName());

    private UiManager uiManager;
    private Client client;

    @Override
    public void init() throws Exception {
        ServerSocket serverSocket = new ServerSocket(Constants.PORT);
        InboundConnectionListener inboundConnectionListener = new InboundConnectionListener(serverSocket);

        DhtClient dhtClient = new DhtClient(Constants.PORT);

        TorrentRepository repository = new FileTorrentRepository();
        PieceRepository pieceRepository = new FilePieceRepository();
        client = new Client(repository, pieceRepository, inboundConnectionListener, new LocalServiceDiscoveryManager(),
                dhtClient);
    }

    @Override
    public void start(Stage primaryStage) {
        uiManager = new UiManager(primaryStage, new ViewModel(client));
        uiManager.show();
    }

    @Override
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping JTorrent");
        client.shutdown();
    }
}
