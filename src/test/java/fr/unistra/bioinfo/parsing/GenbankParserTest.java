package fr.unistra.bioinfo.parsing;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
class GenbankParserTest {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    @Autowired
    private RepliconService repliconService;

    @Test
    void parseGenbankFile() {
        Path p = Paths.get(".","src", "test", "resources", "NC_001700.1.gb");
        assertTrue(GenbankParser.parseGenbankFile(p.toFile()));
        RepliconEntity r = repliconService.getByName("NC_001700");
        assertNotNull(r);
        assertTrue(r.isComputed());
        assertEquals(1, r.getVersion().intValue());
        HierarchyEntity h = r.getHierarchyEntity();
        assertNotNull(h);
        assertEquals("Eukaryota", h.getKingdom());
        assertEquals("Animals", h.getGroup());
        assertEquals("Mammals", h.getSubgroup());
        assertEquals("Felis catus", h.getOrganism());
        assertTrue(r.getDinucleotideCount("GG", Phase.PHASE_0) > 0);
        assertEquals(r.getDinucleotideCount("GG", Phase.PHASE_0), r.getDinucleotideCount("gg", Phase.PHASE_0));
        for(Phase phase : Phase.values()){
            if(phase != Phase.PHASE_2){
                for(String di : CommonUtils.DINUCLEOTIDES){
                    LOGGER.info(di+" "+phase+" = "+r.getDinucleotideCount(di, phase));
                }
            }
            for(String tri : CommonUtils.TRINUCLEOTIDES){
                LOGGER.info(tri+" "+phase+" = "+r.getTrinucleotideCount(tri, phase));
            }
        }
    }
}