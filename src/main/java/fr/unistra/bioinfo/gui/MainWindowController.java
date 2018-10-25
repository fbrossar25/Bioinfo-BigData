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
            LOGGER.error("Erreur lros de la mise à jour de la base de données", e);
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
}
