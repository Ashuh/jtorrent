package jtorrent.presentation;

import java.lang.System.Logger.Level;
import java.net.ServerSocket;

import javafx.application.Application;
import javafx.stage.Stage;
import jtorrent.data.repository.FileTorrentRepository;
import jtorrent.domain.Constants;
import jtorrent.domain.manager.IncomingConnectionManager;
import jtorrent.domain.manager.LocalServiceDiscoveryManager;
import jtorrent.domain.manager.TorrentManager;
import jtorrent.domain.repository.TorrentRepository;
import jtorrent.presentation.manager.UiManager;
import jtorrent.presentation.viewmodel.ViewModel;

public class JTorrent extends Application {

    private static final System.Logger LOGGER = System.getLogger(JTorrent.class.getName());

    private UiManager uiManager;
    private TorrentManager torrentManager;

    @Override
    public void init() throws Exception {
        ServerSocket serverSocket = new ServerSocket(Constants.PORT);
        IncomingConnectionManager incomingConnectionManager = new IncomingConnectionManager(serverSocket);
        TorrentRepository repository = new FileTorrentRepository();
        torrentManager = new TorrentManager(repository, incomingConnectionManager, new LocalServiceDiscoveryManager());
    }

    @Override
    public void start(Stage primaryStage) {
        uiManager = new UiManager(primaryStage, new ViewModel(torrentManager));
        uiManager.show();
    }

    @Override
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping JTorrent");
        torrentManager.shutdown();
    }
}
