package jtorrent.presentation.createnewtorrent.view;

import java.io.File;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

public class CreateNewTorrentDialog extends Dialog<CreateNewTorrentDialog.Result> {

    private final CreateNewTorrentView createNewTorrentView;

    public CreateNewTorrentDialog() {
        createNewTorrentView = new CreateNewTorrentView();
        setDialogPane(createNewTorrentView);
        setTitle("Create New Torrent");
        setResultConverter(this::convertResult);
        setResizable(false);
    }

    private Result convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            File source = createNewTorrentView.getSource();
            String trackers = createNewTorrentView.getTrackers();
            int pieceSize = createNewTorrentView.getPieceSize();
            String comment = createNewTorrentView.getComment();
            return new Result(source, trackers, pieceSize, comment);
        } else {
            return null;
        }
    }

    public record Result(File source, String trackers, int pieceSize, String comment) {
    }
}
