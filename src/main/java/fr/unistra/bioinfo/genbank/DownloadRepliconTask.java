package fr.unistra.bioinfo.genbank;

import com.github.rholder.retry.*;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.common.EventUtils;
import fr.unistra.bioinfo.parsing.GenbankParser;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadRepliconTask extends Task<File> implements Callable<File> {
    private static Logger LOGGER = LogManager.getLogger();

    private static final AtomicInteger fileCount = new AtomicInteger(0);
    private static final AtomicLong readCount = new AtomicLong(0);
    private static final String SESSION_ID = CommonUtils.dateToInt(new Date());
    static{
        LOGGER.info("ID de session : '{}'", SESSION_ID);
    }

    /** Objet utilisé pour synchroniser le parsing */
    private static final Object synchronizedObject = new Object();

    private final List<RepliconEntity> replicons;
    private final String repliconsIds;
    private final Retryer<File> retryer;
    private final RepliconService repliconService;
    private final String fileName;
    private final Boolean failed = Boolean.FALSE;

    public DownloadRepliconTask(@NonNull List<RepliconEntity> replicons, @NonNull RepliconService repliconService) {
        this.replicons = replicons;
        if(this.replicons.size() == 1){
            //En utilisant systématiquement le compteur atomique, on garantis que les threads concurrents n'écrivent pas dans le même fichier
            this.fileName = replicons.get(0).getGenbankName()+"-"+SESSION_ID+"-"+fileCount.incrementAndGet()+".gb";
        }else{
            this.fileName = "replicons-"+SESSION_ID+"-"+fileCount.getAndIncrement()+".gb";
        }
        this.repliconsIds = GenbankUtils.getRepliconsIdsString(replicons);
        this.retryer = RetryerBuilder.<File>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(8))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
        this.repliconService = repliconService;
    }

    private File download() throws IOException{
        File f = CommonUtils.DATAS_PATH.resolve(fileName).toFile();
        if(!f.exists()){
            FileUtils.forceMkdirParent(f);
            if(!f.createNewFile()){
                LOGGER.error("Le fichier '{}' n'as pas pu être créé",f.getPath());
                throw new IOException("Le fichier"+f.getPath()+" n'as pas pu être créé");
            }
        }
        for(RepliconEntity r : replicons){
            r.setFileName(fileName);
            synchronized(synchronizedObject){
                repliconService.save(r);
            }
        }
        try(BufferedReader in = new BufferedReader(new InputStreamReader(GenbankUtils.getGBDownloadURL(replicons).toURL().openStream()))) {
            LOGGER.debug("Téléchargement du replicons '{}' -> '{}' débuté",repliconsIds, f.getPath());
            GenbankUtils.GENBANK_REQUEST_LIMITER.acquire(); //Bloque tant qu'on est pas en dessous du nombre de requête max par seconde
            GenbankParser.parseGenbankFile(in);
            for(RepliconEntity r : replicons){
                r.setFileName(fileName);
                synchronized(synchronizedObject){
                    repliconService.save(r);
                }
                EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_REPLICON_END, r);
            }
            EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_FILE_END);
            LOGGER.info("Téléchargement du replicon '{}' -> '{}' terminé",repliconsIds, f.getPath());
        }catch (IOException e){
            for(RepliconEntity r : replicons){
                r.setFileName(null);
                r.setParsed(false);
                r.setComputed(false);
                synchronized(synchronizedObject){
                    repliconService.save(r);
                }
            }
            LOGGER.error("Erreur lors du téléchargement du replicon '{}' -> '{}'", repliconsIds, f.getPath());
            throw new IOException("Erreur lors du téléchargement du replicon '"+repliconsIds+"' -> '"+f.getPath()+"'", e);
        }
        return f;
    }

    public static long getTotalReaded(){
        return readCount.get();
    }

    @Override
    public File call() {
        try {
            return this.retryer.call(this::download);
        } catch (ExecutionException | RetryException e) {
            for(RepliconEntity r : replicons) {
                EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_FILE_FAILED, r);
            }
            LOGGER.error("Erreur du téléchargement du replicon '{}'", repliconsIds, e);
        }
        return null;
    }
}
