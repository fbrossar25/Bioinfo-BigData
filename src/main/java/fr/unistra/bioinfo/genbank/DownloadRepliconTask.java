package fr.unistra.bioinfo.genbank;

import com.github.rholder.retry.*;
import com.google.common.util.concurrent.RateLimiter;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class DownloadRepliconTask extends Task<File> implements Callable<File> {
    private static Logger LOGGER = LogManager.getLogger();

    private RepliconEntity replicon;
    private RateLimiter rateLimiter;
    private Retryer<File> retryer;

    public DownloadRepliconTask(RepliconEntity replicon, RateLimiter rateLimiter) {
        this.replicon = replicon;
        this.rateLimiter = rateLimiter;
        this.retryer = RetryerBuilder.<File>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
    }

    private File download() throws IOException{
        File f = CommonUtils.RESULTS_PATH.resolve(GenbankUtils.getPathOfReplicon(replicon)).toFile();
        LOGGER.trace("Téléchargement replicon '"+replicon.getName()+"' -> '"+f.getPath()+"'");
        try(InputStream in = GenbankUtils.getGBDownloadURL(replicon).toURL().openStream()) {
            FileUtils.copyToFile(in, f);
        }catch (IOException e){
            throw new IOException("Erreur lors du téléchargement du replicon '"+replicon.getName()+"'", e);
        }
        return f;
    }

    @Override
    public File call() {
        if(rateLimiter != null){
            rateLimiter.tryAcquire();
        }
        try {
            return this.retryer.call(this::download);
        } catch (ExecutionException | RetryException e) {
            LOGGER.error("Erreur du téléchargement du replicon '"+replicon.getName()+"'", e);
        }
        return null;
    }
}
