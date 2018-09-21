package fr.unistra.bioinfo.genbank;

import fr.unistra.bioinfo.CustomTestCase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

class GenbankUtilsTest extends CustomTestCase {

    @Test
    void getNumberOfEntries() {
        for(Reign k : Reign.values()){
            LOGGER.info("Number of "+k.getSearchTable()+" entries : "+GenbankUtils.getNumberOfEntries(k));
        }
    }

    @Disabled("Test long à l'éxectuion")
    @Test
    void createOrganismsTreeStructure(){
        GenbankUtils.createOrganismsTreeStructure(Paths.get(".","Results"), false);
    }
}