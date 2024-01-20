package jtorrent.presentation.view;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import jtorrent.presentation.view.fxml.JTorrentFxmlLoader;

public class PreferencesView implements Initializable {

    private final Map<TreeItem<String>, Node> treeItemToNode = new HashMap<>();

    @FXML
    private DialogPane dialogPane;
    @FXML
    private TreeView<String> treeView;
    @FXML
    private Pane pane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            treeView.setRoot(buildTree());
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        treeView.showRootProperty().set(false);
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Node previousSelectedNode = treeItemToNode.get(oldValue);
            if (previousSelectedNode != null) {
                previousSelectedNode.setVisible(false);
            }

            Node selectedNode = treeItemToNode.get(newValue);
            if (selectedNode != null) {
                selectedNode.setVisible(true);
            }
        });

        // This will prevent the dialog from closing
        dialogPane.lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, Event::consume);
    }

    private TreeItem<String> buildTree() throws IOException {
        JTorrentFxmlLoader loader = new JTorrentFxmlLoader();
        TreeItem<String> root = new TreeItem<>();
        root.setExpanded(true);

        Pane general = new Pane();
        general.setVisible(false);
        general.getChildren().add(new Text("General"));

        TreeItem<String> ui = addPage(root, "UI", null);
        addPage(ui, "Appearance", loader.load("AppearanceView.fxml"));
        addPage(ui, "Behavior", null);
        addPage(root, "Connection", null);
        return root;
    }

    private TreeItem<String> addPage(TreeItem<String> parent, String name, Node node) {
        TreeItem<String> item = new TreeItem<>(name);
        parent.getChildren().add(item);
        if (node != null) {
            node.setVisible(false);
            pane.getChildren().add(node);
            treeItemToNode.put(item, node);
        }
        return item;
    }
}
