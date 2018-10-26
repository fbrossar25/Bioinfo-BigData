package fr.unistra.bioinfo.gui;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.genbank.GenbankUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainWindowController {
    private static Logger LOGGER = LogManager.getLogger();
    private static MainWindowController singleton;

    @FXML public BorderPane panelPrincipal;
    @FXML public MenuBar barreMenu;
    @FXML public Menu menuFichier;
    @FXML public Button btnDemarrer;
    @FXML public MenuItem btnNettoyerDonnees;
    @FXML public MenuItem btnSupprimerFichiersGEnomes;
    @FXML public MenuItem btnQuitter;
    @FXML public TextArea logs;
    @FXML public TreeView<Path> treeView;
    @FXML public ProgressBar progressBar;
    @FXML public Label labelDownload;

    private static boolean init = true;
    private static final String ROOT_FOLDER = "Results";

    //Méthode appelée juste après le constructeur du controleur
    @FXML
    public void initialize(){
        singleton = this;
        TextAreaAppender.setTa(logs);
        LOGGER.debug("treeView.isResizable() -> "+treeView.isResizable());
    }

    @FXML
    public void demarrer(ActionEvent actionEvent){
        btnDemarrer.setDisable(true);
        new Thread(() -> {
            try {
                LOGGER.info("Mise à jour de la base de données");
                GenbankUtils.updateNCDatabase();
                Main.generateOrganismDirectories();
                createArborescence();
                LOGGER.info("Mise à jour terminée");
            } catch (IOException e) {
                LOGGER.error("Erreur lors de la mise à jour de la base de données", e);
            }finally {
                btnDemarrer.setDisable(false);
            }
        }).start();
    }

    @FXML
    public void nettoyerDonnees(ActionEvent actionEvent) {
        LOGGER.info("Nettoyage des données...");
    }

    @FXML
    public void supprimerFichiersGenomes(ActionEvent actionEvent) {
        LOGGER.info("Nettoyage des fichiers genomes...");
    }

    @FXML
    public void quitter(ActionEvent actionEvent) {
        Main.openExitDialog(actionEvent);
    }

    private void createArborescence() throws IOException{
        // create root
        TreeItem<Path> treeItem = new TreeItem<>(Paths.get( ROOT_FOLDER));
        treeItem.setExpanded(true);

        createTree("", treeItem);

        Platform.runLater(() -> treeView.setRoot(treeItem));
    }

    //TODO: Vérifier que la méthode est toujours fonctionnel lors d'un update.
    public static void createTree(String pathToParent, TreeItem<Path> rootItem) throws IOException{
        Path p = Paths.get(pathToParent,rootItem.getValue().toString());
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(p)) {
            for (Path path : directoryStream) {
                int count= path.getNameCount()-1;
                TreeItem<Path> newItem = new TreeItem<Path>(path.getName(count));
                newItem.setExpanded(true);

                rootItem.getChildren().add(newItem);

                if (Files.isDirectory(path)) {
                    createTree(pathToParent+rootItem.getValue().toString()+"/", newItem);
                }
            }
        }
    }

    public static MainWindowController get(){
        return singleton;
    }
}
