package fr.unistra.bioinfo.gui;

import ch.qos.logback.core.Appender;
import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.persistence.manager.HierarchyManager;
import fr.unistra.bioinfo.persistence.manager.RepliconManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

@Controller
public class MainWindowController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainWindowController.class);

    private final HierarchyManager hierarchyService;
    private final RepliconManager repliconService;
    private TextAreaAppender loggerTextAeraAppender;

    @Value("${log.textaera.appender.name}")
    private String textAeraAppenderName;

    @FXML private BorderPane panelPrincipal;
    @FXML private MenuBar barreMenu;
    @FXML private Menu menuFichier;
    @FXML private Button btnMain;
    @FXML private MenuItem btnNettoyerDonnees;
    @FXML private MenuItem btnSupprimerFichiersGEnomes;
    @FXML private MenuItem btnQuitter;
    @FXML private TextArea logs;

    @Autowired
    public MainWindowController(HierarchyManager hierarchyService, RepliconManager repliconService) {
        this.hierarchyService = hierarchyService;
        this.repliconService = repliconService;
    }


    @FXML
    public void initialize(){
        LOGGER.info("initialisation de la fenêtre principale...");
        Logger rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if(textAeraAppenderName != null && rootLogger instanceof ch.qos.logback.classic.Logger){
            Appender taaAppender = ((ch.qos.logback.classic.Logger) rootLogger).getAppender(textAeraAppenderName);
            if(taaAppender instanceof TextAreaAppender){
                loggerTextAeraAppender = (TextAreaAppender) taaAppender;
                loggerTextAeraAppender.setTextAera(logs);
                LOGGER.debug("Appender '"+textAeraAppenderName+"' initialisé");
            }
        }else{
            LOGGER.warn("L'appender '"+textAeraAppenderName+"' n'a pas été trouvé");
        }
    }

    public void demarrer(ActionEvent actionEvent){
        LOGGER.debug("Clic sur le bouton 'démarrer'");
    }

    public void nettoyerDonnees(ActionEvent actionEvent) {
        LOGGER.info("Nettoyage des données...");
    }

    public void supprimerFichiersGenomes(ActionEvent actionEvent) {
        LOGGER.info("Suppression des fichiers genomes...");
    }

    public void quitter(ActionEvent actionEvent) {
        Main.openExitDialog(actionEvent);
    }
}
