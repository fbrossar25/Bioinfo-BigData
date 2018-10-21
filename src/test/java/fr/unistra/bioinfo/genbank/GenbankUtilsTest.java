package fr.unistra.bioinfo.genbank;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class GenbankUtilsTest {
    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    void getNumberOfEntries() {
        for(Reign k : Reign.values()){
            LOGGER.info("Number of "+k.getSearchTable()+" entries : "+GenbankUtils.getNumberOfEntries(k));
        }
    }

    @Test
    void updateDB(){
        try {
            assertFalse(GenbankUtils.updateNCDatabase().isEmpty());
            assertFalse(GenbankUtils.getAllReplicons().isEmpty());
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void downloadAllReplicons(){
        try {
            GenbankUtils.updateNCDatabase();
            CompletableFuture<List<File>> future = new CompletableFuture<>();
            GenbankUtils.downloadAllReplicons(future);
            assertFalse(future.get().isEmpty());
        } catch (IOException | InterruptedException | ExecutionException e) {
            fail(e);
        }
    }


    @Disabled("Test long à l'éxecution")
    @Test
    void createOrganismsTreeStructure(){
        assertTrue(GenbankUtils.createAllOrganismsDirectories(Paths.get(".","Results")));
    }
}