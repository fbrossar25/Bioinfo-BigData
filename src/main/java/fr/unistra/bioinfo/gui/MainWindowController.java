package fr.unistra.bioinfo.gui;

import ch.qos.logback.core.Appender;
import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.common.EventUtils;
import fr.unistra.bioinfo.genbank.GenbankException;
import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.gui.tree.RepliconView;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
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
    private static final String ROOT_FOLDER = "Results";

    private static MainWindowController singleton;
    private static boolean init = true;

    private final HierarchyService hierarchyService;
    private final RepliconService repliconService;
    
    @Value("${log.textaera.appender.name}")
    private String textAeraAppenderName;
    private TextAreaAppender logsAppender;

    @FXML private BorderPane panelPrincipal;
    @FXML private MenuBar barreMenu;
    @FXML private Menu menuFichier;
    @FXML private Button btnDemarrer;
    @FXML private MenuItem btnNettoyerDonnees;
    @FXML private MenuItem btnSupprimerFichiersGEnomes;
    @FXML private MenuItem btnQuitter;
    @FXML private ProgressBar progressBar;
    @FXML private Label downloadLabel;
    @FXML private TextArea logs;
    @FXML private RepliconView treeView;

    private final EventUtils.EventListener GENBANK_METADATA_END_LISTENER = (event -> {
        if(event.getType() == EventUtils.EventType.METADATA_END){
            updateFullTreeView();
        }
    });

    @Autowired
    public MainWindowController(HierarchyService hierarchyService, RepliconService repliconService){
        this.hierarchyService = hierarchyService;
        this.repliconService = repliconService;
    }

    //Méthode appelée juste après le constructeur du controleur
    @FXML
    public void initialize(){
        singleton = this;
        Logger l = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if(textAeraAppenderName != null && l instanceof ch.qos.logback.classic.Logger){
            Appender a = ((ch.qos.logback.classic.Logger) l).getAppender(textAeraAppenderName);
            if(a instanceof TextAreaAppender){
                logsAppender = (TextAreaAppender) a;
                logsAppender.setTextAera(logs);
                LOGGER.info("Logging IHM initialisé");
            }else{
                logsAppender = null;
                logs.setText("L'appender '"+textAeraAppenderName+"' de l'IHM n'a pas pu être trouvé");
                LOGGER.warn("L'appender '{}' de l'IHM n'a pas pu être trouvé", textAeraAppenderName);
            }
        }else{
            logsAppender = null;
            logs.setText("Le logger '"+textAeraAppenderName+"' de l'IHM n'a pas pu être trouvé");
            LOGGER.warn("Le logger '{}' de l'IHM n'a pas pu être trouvé", textAeraAppenderName);
        }
        EventUtils.subscribe(GENBANK_METADATA_END_LISTENER);
        updateFullTreeView();
    }

    @FXML
    public void demarrer(ActionEvent actionEvent){
        btnDemarrer.setDisable(true);
        new Thread(() -> {
            try {
                LOGGER.info("Mise à jour de la base de données");
                GenbankUtils.updateNCDatabase();
                Main.generateOrganismDirectories();
                LOGGER.info("Mise à jour terminée");
            } catch (GenbankException e) {
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
        LOGGER.info("Suppression des fichiers genomes...");
    }

    @FXML
    public void quitter(ActionEvent actionEvent) {
        Main.openExitDialog(actionEvent);
    }

    @FXML
    public void viderLogs(ActionEvent actionEvent){
        logsAppender.clear();
    }

    /**
     * Met à jour l'arbre des replicon avec les données en base.</br>
     * Le bouton 'démarrer' est désactiver pendant cette opération
     */
    private void updateFullTreeView(){
        //TODO afficher une petite popup pour indiquer la progression du chargement
        btnDemarrer.setDisable(true);
        treeView.setDisable(true);
        new Thread(() -> {
            CommonUtils.disableHibernateLogging();
            LOGGER.info("Mise à jour de l'arbre des replicons ({} entrées), veuillez patienter...", repliconService.count());
            repliconService.getAll().parallelStream().forEach(replicon -> treeView.addReplicon(replicon));
            CommonUtils.enableHibernateLogging(true);
            btnDemarrer.setDisable(false);
            treeView.setDisable(false);
        }).start();
    }

    public ProgressBar getProgressBar(){
        return progressBar;
    }

    public Label getDownloadLabel(){
        return downloadLabel;
    }

    public static MainWindowController get(){
        return singleton;
    }
}
