package jtorrent.presentation.view;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class ExceptionAlert extends Alert {

    public ExceptionAlert(String title, String headerText, Exception exception) {
        super(AlertType.ERROR);
        setTitle(title);
        setHeaderText(headerText);
        setContentText(exception.getMessage());
        setStacktrace(exception);
    }

    private void setStacktrace(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);

        var textArea = new TextArea(stringWriter.toString());
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane content = new GridPane();
        content.setMaxWidth(Double.MAX_VALUE);
        content.add(new Label("Full stacktrace:"), 0, 0);
        content.add(textArea, 0, 1);

        getDialogPane().setExpandableContent(content);
    }
}
