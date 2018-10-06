package fr.unistra.bioinfo.gui;

import fr.unistra.bioinfo.Main;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

public class MainWindowController {
    public BorderPane panelPrincipal;
    public MenuBar barreMenu;
    public Menu menuFichier;
    public Button btnMain;
    public MenuItem btnNettoyerDonnees;
    public MenuItem btnSupprimerFichiersGEnomes;
    public MenuItem btnQuitter;
    public TextArea logs;


    public void demarrer(ActionEvent actionEvent){
        TextAreaAppender.setTa(logs);
        Main.generateOrganismDirectories();
    }

    public void nettoyerDonnees(ActionEvent actionEvent) {
    }

    public void supprimerFichiersGenomes(ActionEvent actionEvent) {
    }

    public void quitter(ActionEvent actionEvent) {
        Main.openExitDialog(actionEvent);
    }
}
