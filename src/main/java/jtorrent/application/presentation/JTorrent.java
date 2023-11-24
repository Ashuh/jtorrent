package jtorrent.application.presentation;

import java.lang.System.Logger.Level;
import java.net.ServerSocket;

import javafx.application.Application;
import javafx.stage.Stage;
import jtorrent.application.domain.Client;
import jtorrent.application.presentation.viewmodel.ViewModel;
import jtorrent.common.domain.Constants;
import jtorrent.dht.domain.handler.DhtClient;
import jtorrent.incoming.domain.InboundConnectionListener;
import jtorrent.lsd.domain.handler.LocalServiceDiscoveryManager;
import jtorrent.torrent.data.repository.FilePieceRepository;
import jtorrent.torrent.data.repository.FileTorrentRepository;
import jtorrent.torrent.domain.repository.PieceRepository;
import jtorrent.torrent.domain.repository.TorrentRepository;

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
