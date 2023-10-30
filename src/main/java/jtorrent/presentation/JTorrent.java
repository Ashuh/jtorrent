package jtorrent.presentation;

import java.lang.System.Logger.Level;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import javafx.application.Application;
import javafx.stage.Stage;
import jtorrent.data.repository.FilePieceRepository;
import jtorrent.data.repository.FileTorrentRepository;
import jtorrent.domain.Constants;
import jtorrent.domain.manager.Client;
import jtorrent.domain.manager.IncomingConnectionManager;
import jtorrent.domain.manager.LocalServiceDiscoveryManager;
import jtorrent.domain.manager.dht.DhtManager;
import jtorrent.domain.manager.dht.DhtQueryHandler;
import jtorrent.domain.manager.dht.PeerContactInfoStore;
import jtorrent.domain.manager.dht.routingtable.RoutingTable;
import jtorrent.domain.repository.PieceRepository;
import jtorrent.domain.repository.TorrentRepository;
import jtorrent.domain.socket.DhtSocket;
import jtorrent.presentation.manager.UiManager;
import jtorrent.presentation.viewmodel.ViewModel;

public class JTorrent extends Application {

    private static final System.Logger LOGGER = System.getLogger(JTorrent.class.getName());

    private UiManager uiManager;
    private Client client;

    @Override
    public void init() throws Exception {
        ServerSocket serverSocket = new ServerSocket(Constants.PORT);
        IncomingConnectionManager incomingConnectionManager = new IncomingConnectionManager(serverSocket);

        RoutingTable routingTable = new RoutingTable();
        PeerContactInfoStore peerContactInfoStore = new PeerContactInfoStore();
        DhtSocket.QueryHandler queryHandler = new DhtQueryHandler(routingTable, peerContactInfoStore);
        DhtSocket dhtSocket = new DhtSocket(new DatagramSocket(Constants.PORT), queryHandler);
        DhtManager dhtManager = new DhtManager(dhtSocket, routingTable);

        TorrentRepository repository = new FileTorrentRepository();
        PieceRepository pieceRepository = new FilePieceRepository();
        client = new Client(repository, pieceRepository, incomingConnectionManager, new LocalServiceDiscoveryManager(),
                dhtManager);
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
