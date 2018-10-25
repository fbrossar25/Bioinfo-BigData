package fr.unistra.bioinfo.gui;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.genbank.GenbankUtils;
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
    public static Logger LOGGER = LogManager.getLogger();
    @FXML public BorderPane panelPrincipal;
    @FXML public MenuBar barreMenu;
    @FXML public Menu menuFichier;
    @FXML public Button btnMain;
    @FXML public MenuItem btnNettoyerDonnees;
    @FXML public MenuItem btnSupprimerFichiersGEnomes;
    @FXML public MenuItem btnQuitter;
    @FXML public TextArea logs;
    @FXML public TreeView<Path> treeView;

    private static boolean init = true;
    private static final String ROOT_FOLDER = "Results";

    //Méthode appelée juste après le constructeur du controleur
    @FXML
    public void initialize(){

    }

    @FXML
    public void demarrer(ActionEvent actionEvent){
        TextAreaAppender.setTa(logs);
        try {
            GenbankUtils.updateNCDatabase();
            Main.generateOrganismDirectories();
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la mise à jour de la base de données", e);
        }
        try {
            createArborescence();
        }catch(IOException e){
            LOGGER.error("Erreur lors de la création de l'arborescence");
            e.printStackTrace();
        }
    }

    @FXML
    public void nettoyerDonnees(ActionEvent actionEvent) {
    }

    @FXML
    public void supprimerFichiersGenomes(ActionEvent actionEvent) {
    }

    @FXML
    public void quitter(ActionEvent actionEvent) {
        Main.openExitDialog(actionEvent);
    }

    private void createArborescence() throws IOException{
        // create root
        TreeItem<Path> treeItem = new TreeItem<Path>(Paths.get( ROOT_FOLDER));
        treeItem.setExpanded(true);

        createTree("", treeItem);

        treeView.setRoot(treeItem);
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


}
