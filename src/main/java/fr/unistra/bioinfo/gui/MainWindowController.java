package fr.unistra.bioinfo.gui;

import ch.qos.logback.core.Appender;
import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.common.EventUtils;
import fr.unistra.bioinfo.genbank.GenbankException;
import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.gui.tree.RepliconView;
import fr.unistra.bioinfo.gui.tree.RepliconViewNode;
import fr.unistra.bioinfo.parsing.GenbankParser;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import fr.unistra.bioinfo.stats.OrganismExcelGenerator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import java.io.File;

@Controller
public class MainWindowController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainWindowController.class);
    private static final String ROOT_FOLDER = "Results";

    private static MainWindowController singleton;
    private static boolean init = true;
    private static long nbReplicon = 0;
    private static long countReplicon = 0;

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
    @FXML private ProgressBar progressBarTreeView;
    @FXML private Label downloadLabel;
    @FXML private Label treeViewLabel;
    @FXML private TextArea logs;
    @FXML private RepliconView treeView;

    private final EventUtils.EventListener GENBANK_METADATA_END_LISTENER = (event -> {
        if(event.getType() == EventUtils.EventType.METADATA_END){
            updateFullTreeView();
        }
    });

    private final EventUtils.EventListener STATS_END_LISTENER = (event -> {
        if(event.getType() == EventUtils.EventType.STATS_END){
            RepliconEntity r = event.getReplicon();
            try {
                TreeItem<RepliconViewNode> replicon = treeView.getRepliconNode(r);
                replicon.getValue().setState(RepliconViewNode.RepliconViewNodeState.OK);
                Node n = replicon.getGraphic();
                if(n instanceof ImageView){
                    ((ImageView)n).setImage(replicon.getValue().getState().getImage());
                }
            }catch(NullPointerException e){
                //On ignore les réplicons qui ne sont pas dans l'arbre/défectueux
            }
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
        EventUtils.subscribe(STATS_END_LISTENER);
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
                GenbankUtils.downloadReplicons(repliconService.getNotDownloadedReplicons(), null);
                File dir = CommonUtils.DATAS_PATH.toFile();
                File[] listFiles = dir.listFiles();
                if(listFiles != null) {
                    for (File gb : listFiles) {
                        LOGGER.debug("Parsing file '{}'", gb.getName());
                        GenbankParser.parseGenbankFile(gb);
                    }
                }
                for(HierarchyEntity entity : hierarchyService.getAll()){
                    new OrganismExcelGenerator(entity, this.hierarchyService, this.repliconService).generateExcel();
                }
//                OrganismExcelGenerator o;
//                Page<HierarchyEntity> page = hierarchyService.getAll(0, Sort.Direction.ASC, HierarchyMembers.ORGANISM);
//                while(page.hasNext()){
//                    List<HierarchyEntity> entities = page.getContent();
//                    for(HierarchyEntity entity : entities){
//                        o = new OrganismExcelGenerator(entity, this.hierarchyService, this.repliconService);
//                        o.generateExcel();
//                    }
//                    page = hierarchyService.getAll(page.nextPageable());
//                }
                LOGGER.info("countReplicon = "+ this.countReplicon);
                LOGGER.info("Mise à jour terminée");
            } catch (GenbankException e) {
                LOGGER.error("Erreur lors de la mise à jour de la base de données", e);
            } finally {
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
            this.nbReplicon = repliconService.count();
            this.countReplicon = 0;

            treeView.clear();
            repliconService.getAll().parallelStream().forEach(replicon -> treeView.addReplicon(replicon));
            CommonUtils.enableHibernateLogging(true);
            btnDemarrer.setDisable(false);
            treeView.setDisable(false);

            LOGGER.info("Mise à jour de l'arbre terminée");
        }).start();
    }

    public ProgressBar getProgressBar(){
        return progressBar;
    }

    public Label getDownloadLabel(){
        return downloadLabel;
    }

    public ProgressBar getProgressBarTreeView() { return progressBarTreeView;}

    public Label getTreeViewLabel(){ return treeViewLabel; }

    public static void increaseCounterReplicon(){
        countReplicon++;
    }


    public static MainWindowController get(){
        return singleton;
    }
}
