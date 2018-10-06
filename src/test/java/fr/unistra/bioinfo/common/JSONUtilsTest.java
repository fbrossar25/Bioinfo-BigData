package fr.unistra.bioinfo.common;

import fr.unistra.bioinfo.model.Hierarchy;
import fr.unistra.bioinfo.model.Replicon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JSONUtilsTest {
    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    public void readFromJson() throws URISyntaxException {
        Path p = null;
        try {
            URL url = getClass().getClassLoader().getResource("test.json");
            assertNotNull(url, "Fichier resources/test.json");
            p = Paths.get(url.toURI());
            List<Hierarchy> hierarchies = JSONUtils.readFromFile(p);
            assertEquals(1, hierarchies.size());
            Hierarchy h = hierarchies.get(0);
            assertEquals("TestOrganism", h.getOrganism());
            assertEquals("TestKingdom", h.getKingdom());
            assertEquals("TestGroup", h.getGroup());
            assertEquals("TestSubgroup", h.getSubgroup());
            assertEquals(1, h.getReplicons().size());
            assertTrue(h.getReplicons().containsKey("NC_013023"));
            Replicon r = h.getReplicons().get("NC_013023");
            assertEquals("NC_013023", r.getReplicon());
            assertFalse(r.isDownloaded());
            assertTrue(r.isComputed());
            assertEquals(1, r.getVersion().intValue());
            for(String di : CommonUtils.DINUCLEOTIDES){
                if("CG".equals(di)){
                    assertEquals(21, r.getDinucleotide(di).intValue());
                }else{
                    assertEquals(0, r.getDinucleotide(di).intValue());
                }
            }
            for(String tri : CommonUtils.TRINUCLEOTIDES){
                if("ATT".equals(tri)){
                    assertEquals(1, r.getTrinucleotide(tri).intValue());
                }else{
                    assertEquals(0, r.getTrinucleotide(tri).intValue());
                }
            }
        } catch (IOException e) {
            fail("Erreur lors de la lecture du fichier '"+p.toString()+"'",e);
        }
    }
}