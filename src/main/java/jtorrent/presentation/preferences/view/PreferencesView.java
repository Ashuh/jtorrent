package jtorrent.presentation.preferences.view;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import jtorrent.presentation.common.util.JTorrentFxmlLoader;

public class PreferencesView extends DialogPane {

    private final Map<TreeItem<String>, Node> treeItemToNode = new HashMap<>();

    @FXML
    private TreeView<String> treeView;
    @FXML
    private Pane pane;

    public PreferencesView() {
        try {
            JTorrentFxmlLoader.loadView(this);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @FXML
    public void initialize() {
        treeView.setRoot(buildTree());
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
        lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, Event::consume);
    }

    private TreeItem<String> buildTree() {
        TreeItem<String> root = new TreeItem<>();
        root.setExpanded(true);

        Pane general = new Pane();
        general.setVisible(false);
        general.getChildren().add(new Text("General"));

        TreeItem<String> ui = addPage(root, "UI", null);
        addPage(ui, "Appearance", new AppearanceView());
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
