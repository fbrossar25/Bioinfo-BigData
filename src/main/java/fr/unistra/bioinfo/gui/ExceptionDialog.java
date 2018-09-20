package fr.unistra.bioinfo.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionDialog extends Alert {
    private final Throwable exception;
    private final Thread thread;

    public ExceptionDialog(Throwable e){
        this(Thread.currentThread(), e);
    }

    public ExceptionDialog(Thread t, Throwable e) {
        super(AlertType.ERROR);
        exception = e;
        thread = t;
        setTitle("Erreur");
        setHeaderText("Une erreur est survenue dans le thread '"+(t==null ? "null" : t.getName())+"'");
        setContentText(e == null ? "Erreur null" : e.getMessage());

        String exceptionText = "Pas de stacktrace";
        if(exception != null){
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            exceptionText = sw.toString();
        }

        Label label = new Label("Stacktrace :");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        getDialogPane().setExpandableContent(expContent);

        showAndWait();
    }

    public Throwable getException() {
        return exception;
    }

    public Thread getThread() {
        return thread;
    }
}
