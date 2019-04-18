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
            List<GenbankReader.CDS> cdsValid = gbReader.getListCDSValid();
            assertEquals(1,cdsValid.get(0).begin);
            assertEquals(60,cdsValid.get(0).end);
            assertEquals(61,cdsValid.get(1).begin);
            assertEquals(90,cdsValid.get(1).end);
            assertEquals(91,cdsValid.get(2).begin);
            assertEquals(120,cdsValid.get(2).end);
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }
}