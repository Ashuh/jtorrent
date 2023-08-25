module jtorrent {
    requires java.desktop;
    requires java.logging;
    requires javafx.controls;
    requires javafx.fxml;
    requires io.reactivex.rxjava3;
    requires com.dampcake.bencode;

    opens jtorrent.presentation to javafx.graphics;
    opens jtorrent.presentation.view to javafx.fxml;
}
