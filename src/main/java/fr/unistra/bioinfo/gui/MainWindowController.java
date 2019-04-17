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
import javafx.application.Platform;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    @FXML private ProgressBar progressBarParsing;
    @FXML private Label downloadLabel;
    @FXML private Label parsingLabel;
    @FXML private TextArea logs;
    @FXML private RepliconView treeView;

    private final EventUtils.EventListener GENBANK_METADATA_END_LISTENER = (event -> {
        if(event.getType() == EventUtils.EventType.METADATA_END){
            updateFullTreeView();
        }
    });

    private AtomicInteger countDownload = new AtomicInteger(0);
    private AtomicInteger numberOfFiles = new AtomicInteger(0);

    private final EventUtils.EventListener GENBANK_DOWNLOAD_END = (event -> {
        if(event.getType() == EventUtils.EventType.DOWNLOAD_FILE_END){
            Platform.runLater(() -> {
                this.getProgressBar().setProgress(countDownload.incrementAndGet()/(double)numberOfFiles.get());
                this.getDownloadLabel().setText(countDownload.get() + "/" + numberOfFiles.get() + " fichiers téléchargés ");
            });
        }
    });

    private final EventUtils.EventListener GENBANK_DOWNLOAD_START = (event -> {
        if(event.getType() == EventUtils.EventType.DOWNLOAD_BEGIN){
            Platform.runLater(() -> {
                this.getProgressBar().setProgress(0.0);
                this.getDownloadLabel().setText("0/" + event.getEntityName() + " fichiers téléchargés ");
            });
            numberOfFiles.set(Integer.parseInt(event.getEntityName()));
        }
    });


    private final EventUtils.EventListener STATS_END_LISTENER = (event -> {
        RepliconEntity r = event.getReplicon();
        String entityName = event.getEntityName();
        TreeItem<RepliconViewNode> replicon = null;
        RepliconViewNode.RepliconViewNodeState nextState = null;
        switch(event.getType()){
            case PARSING_BEGIN:
                break;
            case STATS_END:
                replicon = treeView.getRoot();
                break;
            case STATS_END_REPLICON:
                replicon = treeView.getRepliconNode(r);
                nextState = RepliconViewNode.RepliconViewNodeState.OK;
                break;
            case STATS_END_ORGANISM:
                replicon = treeView.getOrganismNode(entityName);
                nextState = RepliconViewNode.RepliconViewNodeState.OK;
                break;
            case STATS_END_SUBGROUP:
                replicon = treeView.getSubgroupNode(entityName);
                break;
            case STATS_END_GROUP:
                replicon = treeView.getGroupNode(entityName);
                break;
            case STATS_END_KINGDOM:
                replicon = treeView.getKingdomNode(entityName);
                break;
            default:
        }
        if(replicon != null){
            if(nextState == null){
                nextState = RepliconViewNode.RepliconViewNodeState.OK;
                for(TreeItem<RepliconViewNode> son : replicon.getChildren()){
                    if(son.getValue().getState() == RepliconViewNode.RepliconViewNodeState.INTERMEDIARY){
                        nextState = RepliconViewNode.RepliconViewNodeState.INTERMEDIARY;
                    }else if(son.getValue().getState() == RepliconViewNode.RepliconViewNodeState.NOK){
                        nextState = RepliconViewNode.RepliconViewNodeState.INTERMEDIARY;
                        break;
                    }
                }
            }
            replicon.getValue().setState(nextState);
            Node n = replicon.getGraphic();
            if(n instanceof ImageView){
                ((ImageView)n).setImage(replicon.getValue().getState().getImage());
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
        EventUtils.subscribe(GENBANK_DOWNLOAD_END);
        EventUtils.subscribe(GENBANK_DOWNLOAD_START);
        updateFullTreeView();
    }

    @FXML
    public void demarrer(ActionEvent actionEvent){
        btnDemarrer.setDisable(true);
        new Thread(() -> {
            try {
                LOGGER.info("Mise à jour de la base de données");
                GenbankUtils.updateNCDatabase();
                GenbankUtils.downloadReplicons(repliconService.getNotDownloadedReplicons(), null);
                File dir = CommonUtils.DATAS_PATH.toFile();
                File[] listFiles = dir.listFiles();
                if(listFiles != null) {
                    for (File gb : listFiles) {
                        LOGGER.debug("Parsing file '{}'", gb.getName());
                        GenbankParser.parseGenbankFile(gb);
                    }
                }
                LOGGER.info("Début de la génération des excels...");
                List<HierarchyEntity> hierarchies = hierarchyService.getAll();
                int count = hierarchies.size();
                Platform.runLater(() -> {
                    this.getProgressBarTreeView().setProgress(0.0);
                    this.getTreeViewLabel().setText( "0/"+count+" organismes traités (génération des excels) ");
                });
                final AtomicInteger atomicCount = new AtomicInteger(0);
                for(HierarchyEntity entity : hierarchyService.getAll()){
                    new OrganismExcelGenerator(entity, this.hierarchyService, this.repliconService).generateExcel();
                    atomicCount.incrementAndGet();
                    if(atomicCount.get() % 100 == 0){
                        LOGGER.info("Generation des feuilles Excel -> {}/{} organismes traités", atomicCount.get(), count);
                    }
                    Platform.runLater(() -> {

                        this.getProgressBarTreeView().setProgress(atomicCount.get()/(double)count);
                        this.getTreeViewLabel().setText(atomicCount.get() + "/" + count + " organismes traités (génération des excels)");

                    });
                }
                LOGGER.info("Génération des excels terminés");
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
        btnDemarrer.setDisable(true);
        treeView.setDisable(true);

        new Thread(() -> {
            CommonUtils.disableHibernateLogging();
            LOGGER.info("Mise à jour de l'arbre des replicons ({} entrées), veuillez patienter...", repliconService.count());

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

    public ProgressBar getProgressBarTreeView() { return progressBarParsing;}

    public Label getTreeViewLabel(){ return parsingLabel; }


    public static MainWindowController get(){
        return singleton;
    }

    public void setNumberOfFiles(int size) {
        this.numberOfFiles.set(size);
    }
}
