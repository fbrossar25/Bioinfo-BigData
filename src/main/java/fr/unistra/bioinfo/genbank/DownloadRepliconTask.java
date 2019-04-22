package fr.unistra.bioinfo.genbank;

import com.github.rholder.retry.*;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.common.EventUtils;
import fr.unistra.bioinfo.parsing.GenbankParser;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadRepliconTask extends Task<RepliconEntity> implements Callable<RepliconEntity> {
    private static Logger LOGGER = LogManager.getLogger();

    private static final AtomicInteger downloadCount = new AtomicInteger(0);
    private static final String SESSION_ID = CommonUtils.dateToInt(new Date());
    static{
        LOGGER.info("ID de session : '{}'", SESSION_ID);
    }

    /** Objet utilisé pour synchroniser le parsing */
    private static final Object synchronizedObject = new Object();

    private final RepliconEntity replicon;
    private final String repliconsIds;
    private final Retryer<RepliconEntity> retryer;
    private final RepliconService repliconService;
    private final String fileName;
    private final Boolean failed = Boolean.FALSE;

    public DownloadRepliconTask(@NonNull RepliconEntity replicon, @NonNull RepliconService repliconService) {
        this.replicon = replicon;
        //En utilisant systématiquement le compteur atomique, on garantis que les threads concurrents n'écrivent pas dans le même fichier
        this.fileName = replicon.getGenbankName()+"-"+SESSION_ID+"-"+downloadCount.incrementAndGet()+".gb";
        this.repliconsIds = replicon.getGenbankName();
        this.retryer = RetryerBuilder.<RepliconEntity>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(8))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
        this.repliconService = repliconService;
    }

    private RepliconEntity download() throws IOException{
        synchronized(synchronizedObject){
            repliconService.save(replicon);
        }
        try(BufferedReader in = new BufferedReader(new InputStreamReader(GenbankUtils.getGBDownloadURL(replicon).toURL().openStream()))) {
            LOGGER.debug("Téléchargement et parsing du replicons '{}' débuté",repliconsIds);
            GenbankUtils.GENBANK_REQUEST_LIMITER.acquire(); //Bloque tant qu'on est pas en dessous du nombre de requête max par seconde
            GenbankParser.parseGenbankFile(in, replicon);
            EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_REPLICON_END, replicon);
            EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_FILE_END);
            LOGGER.info("Téléchargement et parsing du replicon '{}' terminé",replicon.getGenbankName());
        }catch (IOException e){
            replicon.setFileName(null);
            replicon.setParsed(false);
            replicon.setComputed(false);
            synchronized(synchronizedObject){
                repliconService.save(replicon);
            }
            LOGGER.error("Erreur lors du téléchargement du replicon '{}'", replicon.getGenbankName(), e);
            throw new IOException("Erreur lors du téléchargement du replicon '"+replicon.getGenbankName()+"'", e);
        }
        return replicon;
    }

    @Override
    public RepliconEntity call() {
        try {
            return this.retryer.call(this::download);
        } catch (ExecutionException | RetryException e) {
            EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_FILE_FAILED, replicon);
            LOGGER.error("Erreur du téléchargement du replicon '{}'", repliconsIds, e);
        }
        return null;
    }
}
