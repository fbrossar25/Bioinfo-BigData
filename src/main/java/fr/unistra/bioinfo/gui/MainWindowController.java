package fr.unistra.bioinfo.gui;

import fr.unistra.bioinfo.Main;
import javafx.event.ActionEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;

public class MainWindowController {
    public BorderPane panelPrincipal;
    public MenuBar barreMenu;
    public Menu menuFichier;
    public MenuItem btnNettoyerDonnees;
    public MenuItem btnSupprimerFichiersGEnomes;
    public MenuItem btnQuitter;

    public void nettoyerDonnees(ActionEvent actionEvent) {
    }

    public void supprimerFichiersGenomes(ActionEvent actionEvent) {
    }

    public void quitter(ActionEvent actionEvent) {
        Main.openExitDialog(actionEvent);
    }
}
