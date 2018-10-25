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
    public TreeView<String> treeView;

    //Méthode appelée juste après le constructeur du controleur
    @FXML
    public void initialize(){
    public static int count = 0;

    }

    @FXML
    public void demarrer(ActionEvent actionEvent){
        TextAreaAppender.setTa(logs);
        try {
            GenbankUtils.updateNCDatabase();
            Main.generateOrganismDirectories();
        } catch (IOException e) {
            LOGGER.error("Erreur lros de la mise à jour de la base de données", e);
        }
        if(count == 0){
            createArborescence();
        }else{
            count ++;
            updateArborescence();
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

    private void createArborescence(){
        TreeItem<String> root = new TreeItem<String>("Root Node");
        root.setExpanded(true);
        root.getChildren().addAll(new TreeItem<String>("Item 1"), new TreeItem<String>("Item 2"), new TreeItem<String>("Item 3"));
        treeView.setRoot(root);
    }

    private void updateArborescence(){
        TreeItem<String> root = treeView.getRoot();
        root.getChildren().add(new TreeItem<>("Item 4"));
        treeView.setRoot(root);
    }

}
