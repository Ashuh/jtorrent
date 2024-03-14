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

    opens jtorrent.presentation.view to javafx.fxml;
    opens jtorrent.presentation to javafx.graphics;
    opens jtorrent.presentation.component to javafx.fxml;
}
