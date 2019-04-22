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

    private static final Path FELIS_CATUS_PATH = Paths.get("src", "test", "resources", "NC_001700.1.gb");
    private static final Path TEST_FILE = Paths.get("src", "test", "resources", "complement-test.gb");
    private static final Path TEST_MULTILINE = Paths.get("src", "test", "resources", "multiline-of-doom.gb");
    private static final int INVALIDS_CDS = 1;
    private static final int VALIDS_CDS = 4;
    private static final int ORIGIN_LENGTH = 130;
    private static final String EXPECTED_SUBSEQUENCE = "ATTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATAATTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATTT";
    private static final String EXPECTED_SUBSEQUENCE_OF_DOOM = "CGTACGTACGTAC";

    @Test
    void shouldHaveSameNumberOfSubsequencesAndValidsCDS(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(FELIS_CATUS_PATH.toFile(), true);
            gbReader.process();
            assertEquals(7, gbReader.getValidsCDS());
            assertEquals(7, gbReader.getProcessedSubsequences().size());
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }

    @Test
    void shouldParseOrganism(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile(), true);
            gbReader.process();
            assertEquals("Felis catus", gbReader.getOrganism());
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }

    @Test
    void shouldParseMultilineOfDoom(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_MULTILINE.toFile(), true);
            gbReader.process();
            List<StringBuilder> subSeqs = gbReader.getProcessedSubsequences();
            assertNotNull(subSeqs);
            assertEquals(1, subSeqs.size());
            assertEquals(1, gbReader.getValidsCDS());
            assertEquals(0, gbReader.getInvalidsCDS());
            assertEquals(EXPECTED_SUBSEQUENCE_OF_DOOM.length(), subSeqs.stream().mapToInt(StringBuilder::length).sum());
            assertEquals(EXPECTED_SUBSEQUENCE_OF_DOOM, String.join("", subSeqs));
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }

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
    void shouldParseSource(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile());
            gbReader.process();
            assertEquals(ORIGIN_LENGTH, gbReader.getSequenceLength());
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }

    @Test
    void shouldParseCDSSubsequence(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile(), true);
            gbReader.process();
            List<StringBuilder> subSeqs = gbReader.getProcessedSubsequences();
            assertNotNull(subSeqs);
            assertEquals(VALIDS_CDS, subSeqs.size());
            assertEquals(VALIDS_CDS, gbReader.getValidsCDS());
            assertEquals(INVALIDS_CDS, gbReader.getInvalidsCDS());
            assertEquals(EXPECTED_SUBSEQUENCE, String.join("", subSeqs));
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }

    @Test
    void shouldParseCDS(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile(), true);
            gbReader.process();
            assertEquals(VALIDS_CDS, gbReader.getValidsCDS());
            assertEquals(INVALIDS_CDS, gbReader.getInvalidsCDS());
            List<GenbankReader.CDS> cdsValid = gbReader.getListCDSValid();
            assertNotNull(cdsValid);
            assertFalse(cdsValid.isEmpty());
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }
}