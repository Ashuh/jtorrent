package jtorrent.presentation.exception.view;

import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class StackTraceView extends GridPane {

    private final ObjectProperty<Exception> exception = new SimpleObjectProperty<>();

    public StackTraceView() {
        TextArea textArea = new TextArea();
        textArea.textProperty().bind(exception.map(StackTraceView::getStackTrace));
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        setMaxWidth(Double.MAX_VALUE);
        add(new Label("Full stacktrace:"), 0, 0);
        add(textArea, 0, 1);
    }

    private static String getStackTrace(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public void setException(Exception exception) {
        this.exception.set(exception);
    }
}
