module jtorrent {
    requires java.desktop;
    requires java.logging;
    requires javafx.controls;
    requires javafx.fxml;
    requires io.reactivex.rxjava3;
    requires com.dampcake.bencode;
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    opens jtorrent.common.presentation to javafx.fxml;
    opens jtorrent.peer.presentation to javafx.fxml;
    opens jtorrent.torrent.presentation to javafx.fxml;
    opens jtorrent.application.domain to javafx.graphics;
    opens jtorrent.application.presentation to javafx.fxml, javafx.graphics;
    opens jtorrent.application.presentation.view to javafx.fxml, javafx.graphics;
    opens jtorrent.application.presentation.viewmodel to javafx.fxml, javafx.graphics;
    opens jtorrent.torrent.presentation.view to javafx.fxml;
    opens jtorrent.peer.presentation.view to javafx.fxml;
}
