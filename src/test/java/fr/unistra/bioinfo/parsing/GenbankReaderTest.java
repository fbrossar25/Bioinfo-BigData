package fr.unistra.bioinfo.parsing;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenbankReaderTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenbankReaderTest.class);

    private static final Path TEST_FILE = Paths.get("src", "test", "resources", "complement-test.gb");

    @Test
    void shouldParseVersion(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile());
            gbReader.process();
            assertEquals("NC_001700", gbReader.getName());
            assertEquals(1, gbReader.getVersion());
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }


    @Test
    void shouldParseCDS(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile());
            gbReader.process();
            assertEquals(2, gbReader.getValidsCDS());
            assertEquals(0, gbReader.getInvalidsCDS());
            List<StringBuilder> cdsList = gbReader.getProcessedCdsList();
            assertNotNull(cdsList);
            assertEquals(2, cdsList.size());
            assertEquals(cdsList.get(0), cdsList.get(1)); //le CDS 2 est le complement du CDS 1
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }
}