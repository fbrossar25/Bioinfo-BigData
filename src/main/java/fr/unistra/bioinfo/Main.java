package fr.unistra.bioinfo;

import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.gui.ExceptionDialog;
import fr.unistra.bioinfo.gui.MainWindowController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.nio.file.Paths;
import java.util.Optional;

public class Main extends Application {
    private static MainWindowController mainWindowController;
    public static void main(String [] args){
        Thread.setDefaultUncaughtExceptionHandler(Main::defaultErrorHandling);
        launch(args);
    }

    private static void defaultErrorHandling(Thread t, Throwable e){
        new ExceptionDialog(t, e);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            FXMLLoader loader = new FXMLLoader(classLoader.getResource("MainWindow.fxml"));
            Pane root = loader.load();
            mainWindowController = loader.getController();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            initStage(primaryStage);
            primaryStage.show();
            primaryStage.setOnCloseRequest(Main::openExitDialog);
            GenbankUtils.updateNCDatabase();
            GenbankUtils.createAllOrganismsDirectories(Paths.get("Results"));
        } catch (Exception e) {
            new ExceptionDialog(e);
            shutdown();
        }
    }

    public static MainWindowController getMainWindowController(){
        return mainWindowController;
    }

    private void initStage(Stage primaryStage) {
        String version = this.getClass().getPackage().getImplementationVersion();
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setTitle("Cellular Automatons" + ((version == null) ? "" : (" " + version)));
    }

    public static void openExitDialog(Event evt){
        Alert confirmClose = new Alert(Alert.AlertType.CONFIRMATION);
        confirmClose.setTitle("Confirmation");
        confirmClose.setHeaderText("Confirmer la fermeture");
        confirmClose.setContentText("Voulez-vous fermer le logiciel ?");
        Optional<ButtonType> closeResponse = confirmClose.showAndWait();
        if (!closeResponse.isPresent() || !ButtonType.OK.equals(closeResponse.get())) {
            evt.consume();
        } else {
            shutdown();
        }
    }

    public static void shutdown(){
        Platform.exit();
        System.exit(0);
    }
}
