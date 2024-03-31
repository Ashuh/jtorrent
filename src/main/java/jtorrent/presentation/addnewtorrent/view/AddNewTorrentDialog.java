package jtorrent.presentation.addnewtorrent.view;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import jtorrent.domain.torrent.model.TorrentMetadata;
import jtorrent.presentation.addnewtorrent.viewmodel.AddNewTorrentViewModel;

public class AddNewTorrentDialog extends Dialog<AddNewTorrentDialog.Result> {

    private final AddNewTorrentView addNewTorrentView;

    public AddNewTorrentDialog(TorrentMetadata torrentMetadata) {
        addNewTorrentView = new AddNewTorrentView();
        AddNewTorrentViewModel viewModel = new AddNewTorrentViewModel(torrentMetadata);
        addNewTorrentView.setViewModel(viewModel);
        setDialogPane(addNewTorrentView);
        setTitle(viewModel.getName() + " - Add New Torrent");
        setResultConverter(this::convertResult);
        setResizable(true);
    }

    private Result convertResult(ButtonType buttonType) {
        if (buttonType == ButtonType.OK) {
            String name = addNewTorrentView.getName();
            String saveDir = addNewTorrentView.getSaveDirectory();
            return new Result(name, saveDir);
        } else {
            return null;
        }
    }

    public record Result(String name, String saveDirectory) {
    }
}
