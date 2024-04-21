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
    requires org.slf4j;

    opens jtorrent.presentation to javafx.graphics;
    opens jtorrent.presentation.common.component to javafx.fxml;
    opens jtorrent.presentation.main.view to javafx.fxml;
    opens jtorrent.presentation.addnewtorrent.view to javafx.fxml;
    opens jtorrent.presentation.peerinput.view to javafx.fxml;
    opens jtorrent.presentation.createnewtorrent.view to javafx.fxml;
    opens jtorrent.presentation.exception.view to javafx.fxml;
    opens jtorrent.presentation.preferences.view to javafx.fxml;
}
