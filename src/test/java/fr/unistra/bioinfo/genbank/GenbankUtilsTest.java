package fr.unistra.bioinfo.genbank;

import fr.unistra.bioinfo.CustomTestCase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GenbankUtilsTest extends CustomTestCase {

    private static final String TEST_STRING = "uirhouirhvgetrh3568rtj866t8j74rjurs{";

    @Test
    void getNumberOfEntries() {
        for(Reign k : Reign.values()){
            LOGGER.info("Number of "+k.getSearchTable()+" entries : "+GenbankUtils.getNumberOfEntries(k));
        }
    }

    @Disabled("Test long à l'éxecution")
    @Test
    void createOrganismsTreeStructure(){
        assertTrue(GenbankUtils.createOrganismsTreeStructure(Paths.get(".","Results"), true));
    }
}