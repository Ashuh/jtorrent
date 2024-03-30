package jtorrent.presentation.peerinput.view;

import java.io.IOException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;

public class PeerInputView extends DialogPane {

    private static final int PORT_MAX_VALUE = 65535;

    @FXML
    private TextField ip;
    @FXML
    private TextField port;

    public PeerInputView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public String getIp() {
        return ip.getText();
    }

    public String getPort() {
        return port.getText();
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> ip.requestFocus());

        port.setTextFormatter(new TextFormatter<>(new ChangeUnaryOperator(PORT_MAX_VALUE)));

        ObservableValue<Boolean> anyFieldEmpty = ip.textProperty().isEmpty()
                .or(port.textProperty().isEmpty());
        lookupButton(ButtonType.OK).disableProperty().bind(anyFieldEmpty);
    }

    private record ChangeUnaryOperator(int maxValue) implements UnaryOperator<TextFormatter.Change> {

        private static final Pattern DIGIT_PATTERN = Pattern.compile("^\\d*$");

        @Override
        public TextFormatter.Change apply(TextFormatter.Change change) {
            if (!change.isAdded()) {
                return change;
            }

            if (!DIGIT_PATTERN.matcher(change.getText()).matches()) {
                return null;
            }

            String newText = change.getControlNewText();
            int newValue = Integer.parseInt(newText);
            if (newValue > maxValue) {
                return null;
            }

            return change;
        }
    }
}
