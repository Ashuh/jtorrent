package jtorrent.presentation.peerinput.view;

import java.io.IOException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;

public class PeerInputView extends DialogPane {

    private static final int IP_OCTET_MAX_VALUE = 255;
    private static final int PORT_MAX_VALUE = 65535;

    @FXML
    private ChoiceBox<String> choiceBox;
    @FXML
    private TextField ip1;
    @FXML
    private TextField ip2;
    @FXML
    private TextField ip3;
    @FXML
    private TextField ip4;
    @FXML
    private TextField port;

    public PeerInputView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public String getSocketAddress() {
        return ip1.getText() + "." + ip2.getText() + "." + ip3.getText() + "." + ip4.getText() + ":" + port.getText();
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> ip1.requestFocus());

        ip1.setTextFormatter(new TextFormatter<>(new ChangeUnaryOperator(ip2, IP_OCTET_MAX_VALUE)));
        ip2.setTextFormatter(new TextFormatter<>(new ChangeUnaryOperator(ip3, IP_OCTET_MAX_VALUE)));
        ip3.setTextFormatter(new TextFormatter<>(new ChangeUnaryOperator(ip4, IP_OCTET_MAX_VALUE)));
        ip4.setTextFormatter(new TextFormatter<>(new ChangeUnaryOperator(port, IP_OCTET_MAX_VALUE)));
        port.setTextFormatter(new TextFormatter<>(new ChangeUnaryOperator(null, PORT_MAX_VALUE)));

        choiceBox.setItems(FXCollections.observableArrayList("IPv4"));
        choiceBox.getSelectionModel().selectFirst();
        choiceBox.setDisable(true); // disable until IPv6 is supported

        ObservableValue<Boolean> anyFieldEmpty = ip1.textProperty().isEmpty()
                .or(ip2.textProperty().isEmpty())
                .or(ip3.textProperty().isEmpty())
                .or(ip4.textProperty().isEmpty())
                .or(port.textProperty().isEmpty());
        lookupButton(ButtonType.OK).disableProperty().bind(anyFieldEmpty);
    }

    private record ChangeUnaryOperator(TextField next, int maxValue) implements UnaryOperator<TextFormatter.Change> {

        private static final Pattern DIGIT_PATTERN = Pattern.compile("^\\d*$");

        @Override
        public TextFormatter.Change apply(TextFormatter.Change change) {
            if (!change.isAdded()) {
                return change;
            }

            if (".".equals(change.getText())) {
                focusNext();
                return null;
            }

            if (!DIGIT_PATTERN.matcher(change.getText()).matches()) {
                return null;
            }

            String newText = change.getControlNewText();
            int newValue = Integer.parseInt(newText);
            if (newValue > maxValue) {
                return null;
            }

            if (newValue == 0 || newValue * 10 > maxValue) {
                focusNext();
            }

            return change;
        }

        private void focusNext() {
            if (next != null) {
                next.requestFocus();
            }
        }
    }
}
