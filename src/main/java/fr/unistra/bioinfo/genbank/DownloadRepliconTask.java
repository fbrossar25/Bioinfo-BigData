package fr.unistra.bioinfo.genbank;

import com.github.rholder.retry.*;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadRepliconTask extends Task<File> implements Callable<File> {
    private static Logger LOGGER = LogManager.getLogger();

    public static final AtomicInteger fileCount = new AtomicInteger(0);

    /** Objet utilisé pour synchroniser le parsing */
    private static final Object synchronizedObject = new Object();

    private final List<RepliconEntity> replicons;
    private final String repliconsIds;
    private final Retryer<File> retryer;
    private final RepliconService repliconService;

    public DownloadRepliconTask(@NonNull List<RepliconEntity> replicons, @NonNull RepliconService repliconService) {
        this.replicons = replicons;
        this.repliconsIds = GenbankUtils.getRepliconsIdsString(replicons);
        this.retryer = RetryerBuilder.<File>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
        this.repliconService = repliconService;
    }

    private File download() throws IOException{
        File f = CommonUtils.DATAS_PATH.resolve("replicons-"+fileCount.getAndIncrement()+".gb").toFile();
        LOGGER.trace("Téléchargement des replicons '{}' -> '{}' débuté",repliconsIds, f.getPath());
        try(InputStream in = GenbankUtils.getGBDownloadURL(replicons).toURL().openStream()) {
            FileUtils.copyToFile(in, f);
            for(RepliconEntity r : replicons){
                r.setDownloaded(true);
                synchronized(synchronizedObject){
                    repliconService.save(r);
                }
            }
            LOGGER.trace("Téléchargement des replicons '{}' -> '{}' terminé",repliconsIds, f.getPath());
        }catch (IOException e){
            for(RepliconEntity r : replicons){
                r.setDownloaded(false);
                synchronized(synchronizedObject){
                    repliconService.save(r);
                }
            }
            LOGGER.error("Erreur lors du téléchargement des replicons '{}' -> '{}'", repliconsIds, f.getPath());
            throw new IOException("Erreur lors du téléchargement des replicons '"+repliconsIds+"' -> '"+f.getPath()+"'", e);
        }
        return f;
    }

    @Override
    public File call() {
        GenbankUtils.GENBANK_REQUEST_LIMITER.acquire(); //Bloque tant qu'on est pas en dessous du nombre de requête max par seconde
        try {
            return this.retryer.call(this::download);
        } catch (ExecutionException | RetryException e) {
            LOGGER.error("Erreur du téléchargement du replicon '{}'", repliconsIds, e);
        }
        return null;
    }
}
