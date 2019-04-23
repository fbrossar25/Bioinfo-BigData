package fr.unistra.bioinfo.genbank;

import com.github.rholder.retry.*;
import com.google.common.util.concurrent.RateLimiter;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DownloadRepliconTask extends Task<RepliconEntity> implements Callable<RepliconEntity> {
    private static Logger LOGGER = LogManager.getLogger();

    /** Objet utilisé pour synchroniser le parsing */
    private static final Object synchronizedObject = new Object();

    private final RepliconEntity replicon;
    private final String repliconsIds;
    private final Retryer<RepliconEntity> retryer;
    private final RepliconService repliconService;
    private static final RateLimiter PARSING_RATE_LIMITER = RateLimiter.create(10);

    public DownloadRepliconTask(@NonNull RepliconEntity replicon, @NonNull RepliconService repliconService) {
        this.replicon = replicon;
        //En utilisant systématiquement le compteur atomique, on garantis que les threads concurrents n'écrivent pas dans le même fichier
        this.repliconsIds = replicon.getGenbankName();
        this.retryer = RetryerBuilder.<RepliconEntity>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(2))
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
                .build();
        this.repliconService = repliconService;
    }

    private RepliconEntity download() throws IOException{
        GenbankUtils.GENBANK_REQUEST_LIMITER.acquire(); //Bloque tant qu'on est pas en dessous du nombre de requête max par seconde
        try(BufferedReader in = new BufferedReader(new InputStreamReader(GenbankUtils.getGBDownloadURL(replicon).toURL().openStream()))) {
            LOGGER.debug("Téléchargement et parsing du replicons '{}' débuté",repliconsIds);
            PARSING_RATE_LIMITER.acquire();
            GenbankParser.parseGenbankFile(in, replicon);
            EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_REPLICON_END, replicon);
            EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_FILE_END);
            LOGGER.info("Téléchargement et parsing du replicon '{}' terminé",replicon.getGenbankName());
        }catch (IOException e){
            replicon.setParsed(false);
            replicon.setComputed(false);
            synchronized(synchronizedObject){
                repliconService.save(replicon);
            }
            LOGGER.error("Erreur lors du téléchargement du replicon '{}'", replicon.getGenbankName(), e);
            throw new IOException("Erreur lors du téléchargement du replicon '"+replicon.getGenbankName()+"'", e);
        }catch(OutOfMemoryError e){
            replicon.setParsed(false);
            replicon.setComputed(false);
            synchronized(synchronizedObject){
                repliconService.save(replicon);
            }
            EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_FILE_FAILED, replicon);
            LOGGER.error("Erreur lors du téléchargement du replicon '{}'", replicon.getGenbankName(), e);
            return null;
        }
        return replicon;
    }

    @Override
    public RepliconEntity call() {
        try {
            return this.retryer.call(this::download);
        } catch (ExecutionException | RetryException e) {
            replicon.setParsed(false);
            replicon.setComputed(false);
            synchronized(synchronizedObject){
                repliconService.save(replicon);
            }
            EventUtils.sendEvent(EventUtils.EventType.DOWNLOAD_FILE_FAILED, replicon);
            LOGGER.error("Erreur du téléchargement du replicon '{}'", repliconsIds, e);
        }
        return null;
    }
}
