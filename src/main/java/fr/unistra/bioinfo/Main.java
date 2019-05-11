package fr.unistra.bioinfo;

import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.gui.ExceptionDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Optional;

@SpringBootApplication
public class Main extends Application {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static Main mainInstance;
    private ConfigurableApplicationContext springContext;

    public static void main(String [] args){
        Thread.setDefaultUncaughtExceptionHandler(Main::defaultErrorHandling);
        launch(args);
    }

    private static void defaultErrorHandling(Thread t, Throwable e){
        LOGGER.error("Erreur non gérée", e);
        new ExceptionDialog(t, e);
    }

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler(Main::defaultErrorHandling);
        try {
            FXMLLoader loader = new FXMLLoader(CommonUtils.getResourceURL("fxml/MainWindow.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            initStage(primaryStage);
            primaryStage.setOnCloseRequest(Main::openExitDialog);
            primaryStage.setResizable(false);
            primaryStage.setMinHeight(600);
            primaryStage.setMinWidth(1000);
            primaryStage.show();
        } catch (Exception e) {
            new ExceptionDialog(e);
            Main.closeApplication(-1);
        }
    }

    @Override
    public void init() {
        Main.mainInstance = this;
        springContext = SpringApplication.run(Main.class, getParameters().getRaw().toArray(new String[0]));
    }

    private void initStage(Stage primaryStage) {
        String version = this.getClass().getPackage().getImplementationVersion();
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setTitle("BioInfo" + ((version == null) ? "" : (" " + version)));
        primaryStage.getIcons().add(new Image("icon.png"));
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
            Main.closeApplication();
        }
    }

    public static void closeApplication(int status){
        LOGGER.info("Fermeture de l'application...");
        if(Main.mainInstance != null){
            Main.mainInstance.stop();
        }
        Platform.exit();
        System.exit(status);
    }

    public static void closeApplication(){
        Main.closeApplication(0);
    }

    @Override
    public void stop() {
        LOGGER.info("Fermeture du contexte Springboot...");
        springContext.stop();
    }
}
