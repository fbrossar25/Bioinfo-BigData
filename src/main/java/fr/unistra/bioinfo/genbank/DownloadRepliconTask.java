package fr.unistra.bioinfo.genbank;

import com.github.rholder.retry.*;
import com.google.common.util.concurrent.RateLimiter;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class DownloadRepliconTask extends Task<File> implements Callable<File> {
    private static Logger LOGGER = LogManager.getLogger();

    /** Objet utilisé pour synchroniser le parsing */
    private static final Object synchronizedObject = new Object();

    private final RepliconEntity replicon;
    private final RateLimiter rateLimiter;
    private final Retryer<File> retryer;
    private final RepliconService repliconService;

    public DownloadRepliconTask(@NonNull RepliconEntity replicon, @NonNull RateLimiter rateLimiter, @NonNull RepliconService repliconService) {
        this.replicon = replicon;
        this.rateLimiter = rateLimiter;
        this.retryer = RetryerBuilder.<File>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
        this.repliconService = repliconService;
    }

    private File download() throws IOException{
        File f = CommonUtils.DATAS_PATH.resolve(replicon.getFileName()).toFile();
        LOGGER.trace("Téléchargement replicon '"+replicon.getName()+"' -> '"+f.getPath()+"'");
        try(InputStream in = GenbankUtils.getGBDownloadURL(replicon).toURL().openStream()) {
            FileUtils.copyToFile(in, f);
            replicon.setDownloaded(true);
            synchronized(synchronizedObject){
                repliconService.save(replicon);
            }
        }catch (IOException e){
            replicon.setDownloaded(false);
            synchronized(synchronizedObject){
                repliconService.save(replicon);
            }
            throw new IOException("Erreur lors du téléchargement du replicon '"+replicon.getName()+"'", e);
        }
        return f;
    }

    @Override
    public File call() {
        rateLimiter.acquire(); //Bloque tant qu'on est pas en dessous du nombre de requête max par seconde
        try {
            return this.retryer.call(this::download);
        } catch (ExecutionException | RetryException e) {
            LOGGER.error("Erreur du téléchargement du replicon '"+replicon.getName()+"'", e);
        }
        return null;
    }
}
