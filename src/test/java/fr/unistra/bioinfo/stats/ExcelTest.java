package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.parsing.GenbankParser;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
public class ExcelTest {
    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static final String ORGA_NAME = "Felis catus";
    public static final String RESULT_FILENAME = "Results/hello.xlsx";

    private static final Path GENBANK_TEST_FILE_PATH = Paths.get(".","src", "test", "resources", "NC_001700.1.gb");

    @Autowired
    private HierarchyService hierarchyService;
    @Autowired
    private RepliconService repliconService;

    public HierarchyEntity ORGA = null;
    @BeforeEach
    void beforeEach(){
        GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile());
        this.ORGA = hierarchyService.getByOrganism(ORGA_NAME);
    }



    @Test
    void generate_general_informations() {
        XSSFWorkbook wb = new XSSFWorkbook();

        LOGGER.info(ORGA.getOrganism());

        List<RepliconEntity> rr = repliconService.getByHierarchy(ORGA);

        GeneralInformationSheet info = new GeneralInformationSheet(wb, ORGA, rr, GeneralInformationSheet.LEVEL.ORGANISM);
        info.write_lines();

        for ( RepliconEntity r : rr )
        {
            RepliconSheet a = new RepliconSheet(wb, r);
            a.write_sheet();
        }


        FileOutputStream fos = null;
        File f = new File(RESULT_FILENAME);
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*assertTrue(GenbankParser.parseGenbankFile(GENBANK_BACTH_TEST_FILE_PATH.toFile()));
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile()));
        assertEquals(5, repliconService.count().intValue());
        List<RepliconEntity> replicons = repliconService.getAll();
        for(RepliconEntity r : replicons){
            checkReplicon(r);
        }*/
    }


}
