package fr.unistra.bioinfo.genbank;

import com.github.rholder.retry.*;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.model.Replicon;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DownloadRepliconService extends AbstractExecutionThreadService {
    private static Logger LOGGER = LogManager.getLogger();

    private Replicon replicon;
    private Retryer<Void> retryer;
    private final List<File> files;

    public DownloadRepliconService(Replicon replicon, List<File> files) {
        this.replicon = replicon;
        this.files = files;
        this.retryer = RetryerBuilder.<Void>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fibonacciWait())
                .build();
    }

    private Void download() throws IOException{
        File f = CommonUtils.RESULTS_PATH.resolve(GenbankUtils.getPathOfReplicon(replicon)).toFile();
        LOGGER.debug("Télécahrgement du fichier '"+f.getPath()+"'");
        try(InputStream in = GenbankUtils.getGBDownloadURL(replicon).toURL().openStream()) {
            FileUtils.copyToFile(in, f);
        }catch (IOException e){
            throw new IOException("Erreur lors du téléchargement du replicon '"+replicon.getReplicon()+"'", e);
        }
        files.add(f);
        return null;
    }

    @Override
    protected void run() {
        try {
            this.retryer.call(this::download);
        } catch (ExecutionException | RetryException e) {
            LOGGER.error("Erreur du téléchargement du replicon '"+replicon.getReplicon()+"'", e);
        }
    }
}
