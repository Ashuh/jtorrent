package jtorrent.presentation.exception.view;

import javafx.scene.control.Alert;

public class ExceptionAlert extends Alert {

    public ExceptionAlert(String title, String headerText, Exception exception) {
        super(AlertType.ERROR);
        setTitle(title);
        setHeaderText(headerText);
        setContentText(exception.getMessage());
        StackTraceView stackTraceView = new StackTraceView();
        stackTraceView.setException(exception);
        getDialogPane().setExpandableContent(stackTraceView);
    }
}
