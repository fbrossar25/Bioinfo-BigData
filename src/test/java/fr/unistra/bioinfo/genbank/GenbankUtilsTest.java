package fr.unistra.bioinfo.genbank;

import fr.unistra.bioinfo.CustomTestCase;
import org.junit.jupiter.api.Test;

class GenbankUtilsTest extends CustomTestCase {

    @Test
    void getNumberOfEntries() {
        for(Reign k : Reign.values()){
            LOGGER.info("Number of "+k.getlabel()+" entries : "+GenbankUtils.getNumberOfEntries(k));
        }
    }
}