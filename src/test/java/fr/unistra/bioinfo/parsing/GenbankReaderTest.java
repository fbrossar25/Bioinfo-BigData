package fr.unistra.bioinfo.parsing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenbankReaderTest {
    private static final Path TEST_FILE = Paths.get("src", "test", "resources", "test-fix-count.gb");
    private static final int INVALIDS_CDS = 1;
    private static final int VALIDS_CDS = 3;
    private static final int ORIGIN_LENGTH = 2640;
    private static final String EXPECTED_SUBSEQUENCE = "ATACTAATGACTAATCAGCCCATGATCACACATAACTGTGGTGTCATGCATTTGGTATTTTTTATTTTTAGGGGGTCGTAGATATATTGTTGCTGGGCTTATTCTCTATGCGGGGTCTCCACACGCACAGACAGTCAGGGTGCTATTCAGTCAATGGTCACAGGACATATACTTAAATTCCTATTGTTCCACAGGACACGGGATGCGCGCACCCACGTTTGCGGTACACACGTACACACGTACACACGTACACACGTACACACGTACACACGTACACACGTACACACGTACGTACACACGTACACACGTACACACGTACACACGTACACACGTACACACGTACACACGTACACACGTGTACACGTACACACGTACACACGTGTACACGTAGGTCTTAGCTGTCGTGTAGTCGGAGGTGTTAAAGTTACTTTCGTGCTGTATTTTTATGTTAACTGTAGCTTTCTACGGCCTAGTTAGTATATTTTTTTCTCTGTAACACGCTTTACGCCGTGGGTCTATTAGTTTGGGTTAATCGTATGGCCGCGGTGGCTGGCACGAAATTTACCAACCCTTGTTTAATATAGCTTAGTCGAACTTTCATTCATGGCTTAATTTTTATCACTGCTGTATCCCGTGGGGGTGTGGCTGAGCAAGGTGTTATGAGCTACTGTGGTTGTGTGCTTGATACCAGCTCCTTTAGGTCACTGGGTGACTTAGAGGGCATTTTCACCGGGATGCGGAGGCTTGCATGTGTAATCTTATTAATAACTAATGGAAAGGCCAGGACCAAACCTTTGTGTTTATGGAGTCTGGCGACTCATCTAGGCATTTTCAGTGCCTTGCTTTATATGTTTAAGCTACATTAACTGGGATGTGGGGCTAACGTAGACATGTAAAATGTTGTTCGATAGGGACGAATTAGAGTCAGGTCGGTATATCTATAGATAACTGCGAACAGAGTTATGATTTAGTATTAGTAGCTAGTAATAATAGGGGATTGGTTAA";
    private static final int SEQUENCE_LENGTH = EXPECTED_SUBSEQUENCE.length();

    @Test
    void shouldParseOrganism(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile());
            gbReader.process();
            assertEquals("Felis catus", gbReader.getOrganism());
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
            assertEquals(ORIGIN_LENGTH, gbReader.getOriginLength());
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }

    @Test
    void shouldParseCDSSequence(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile());
            gbReader.process();
            StringBuilder subSeq = gbReader.getProcessedSequence();
            assertNotNull(subSeq);
            assertEquals(SEQUENCE_LENGTH, subSeq.length());
            assertEquals(VALIDS_CDS, gbReader.getValidsCDS());
            assertEquals(INVALIDS_CDS, gbReader.getInvalidsCDS());
            assertEquals(EXPECTED_SUBSEQUENCE, subSeq.toString());
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier",e);
        }
    }

    @Test
    void shouldParseCDS(){
        try {
            GenbankReader gbReader = GenbankReader.createInstance(TEST_FILE.toFile());
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