package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.parsing.GenbankParser;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.io.FileUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
public class ExcelTest {
    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static String TEST_PATH = "test_results";

    public static final String ORGA_NAME = "Felis catus";

    private static final Path GENBANK_TEST_FILE_PATH = Paths.get(".","src", "test", "resources", "NC_001700.1.gb");

    @Autowired
    private HierarchyService hierarchyService;
    @Autowired
    private RepliconService repliconService;

    private HierarchyEntity orga = null;
    private List<RepliconEntity> repls = null;

    @BeforeEach
    void beforeEach(){
        GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile());
        this.orga = hierarchyService.getByOrganism(ORGA_NAME);
        this.repls = repliconService.getByHierarchy(this.orga);

        try {
            FileUtils.forceMkdir(new File(TEST_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    void afterEach()
    {
        CommonUtils.disableHibernateLogging();
        hierarchyService.deleteAll();
        repliconService.deleteAll();
        CommonUtils.enableHibernateLogging(true);
    }



    @Test
    void generate_general_informations_ORGANISM() {
        XSSFWorkbook wb = new XSSFWorkbook();

        LOGGER.info(orga.getOrganism());

        GeneralInformationSheet info = new GeneralInformationSheet(wb, orga, this.repls);
        info.write_lines();

        FileOutputStream fos = null;
        File f = new File( TEST_PATH + "/general.xlsx");
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void generate_general_informations_SUB_GROUP() {
        XSSFWorkbook wb = new XSSFWorkbook();

        this.repls.add(this.repls.get(0));

        GeneralInformationSheet info = new GeneralInformationSheet(wb, Arrays.asList(orga), this.repls, GeneralInformationSheet.LEVEL.SUB_GROUP);
        info.write_lines();

        FileOutputStream fos = null;
        File f = new File( TEST_PATH + "/general.SUB_GROUP.xlsx");
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void generate_general_informations_GROUP() {
        XSSFWorkbook wb = new XSSFWorkbook();

        this.repls.add(this.repls.get(0));
        this.repls.add(this.repls.get(0));
        this.repls.add(this.repls.get(0));

        GeneralInformationSheet info = new GeneralInformationSheet(wb, Arrays.asList(orga), this.repls, GeneralInformationSheet.LEVEL.GROUP);
        info.write_lines();

        FileOutputStream fos = null;
        File f = new File( TEST_PATH + "/general.GROUP.xlsx");
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void generate_general_informations_KINDOM() {
        long t = System.currentTimeMillis();
        XSSFWorkbook wb = new XSSFWorkbook();

        this.repls.add(this.repls.get(0));
        this.repls.add(this.repls.get(0));
        this.repls.add(this.repls.get(0));
        this.repls.add(this.repls.get(0));
        this.repls.add(this.repls.get(0));
        this.repls.add(this.repls.get(0));
        this.repls.add(this.repls.get(0));

        GeneralInformationSheet info = new GeneralInformationSheet(wb, Arrays.asList(orga), this.repls, GeneralInformationSheet.LEVEL.KINGDOM);
        info.write_lines();

        FileOutputStream fos = null;
        File f = new File( TEST_PATH + "/general.KINGDOM.xlsx");
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        long tt = System.currentTimeMillis();

        LOGGER.info("TIME -> " + (tt - t) + "s");
    }

    @Test
    void generate_CDS_sheet() {
        XSSFWorkbook wb = new XSSFWorkbook();

        for ( RepliconEntity r : this.repls )
        {
            RepliconSheet a = new RepliconSheet(wb, r);
            a.write_sheet();
        }

        FileOutputStream fos = null;
        File f = new File(TEST_PATH + "/cds_sheet.xlsx");
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void generate_CDS_sheet_SUB_GROUP() {
        XSSFWorkbook wb = new XSSFWorkbook();

        RepliconSheet a = new RepliconSheet(wb, this.repls, GeneralInformationSheet.LEVEL.SUB_GROUP);
        a.write_sheet();

        FileOutputStream fos = null;
        File f = new File(TEST_PATH + "/cds_sheet_SUB_GROUP.xlsx");
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOGGER.warn(TEST_PATH + "/" + this.orga.getKingdom() + "/" + this.orga.getGroup() + "/" + this.orga.getSubgroup() + "/" + this.orga.getOrganism() + ".xlsx");
    }

    @Test
    void test_path_generator()
    {
        assertEquals(
                OrganismExcelGenerator.generate_path(this.orga, TEST_PATH, GeneralInformationSheet.LEVEL.ORGANISM),
                "test_results/Eukaryota/Animals/Mammals/Felis catus.xlsx"
        );
        assertEquals(
                OrganismExcelGenerator.generate_path(this.orga, TEST_PATH, GeneralInformationSheet.LEVEL.SUB_GROUP),
                "test_results/Eukaryota/Animals/Mammals.xlsx"
        );
        assertEquals(
                OrganismExcelGenerator.generate_path(this.orga, TEST_PATH, GeneralInformationSheet.LEVEL.GROUP),
                "test_results/Eukaryota/Animals.xlsx"
        );
        assertEquals(
                OrganismExcelGenerator.generate_path(this.orga, TEST_PATH, GeneralInformationSheet.LEVEL.KINGDOM),
                "test_results/Eukaryota.xlsx"
        );
    }

    @Test
    void test_excel_organism_write()
    {
        OrganismExcelGenerator o = new OrganismExcelGenerator(this.orga, TEST_PATH, this.hierarchyService, this.repliconService);
        o.generate_excel_organism();
    }

    @Test
    void test_generateExcel()
    {
        OrganismExcelGenerator o = new OrganismExcelGenerator(this.orga, TEST_PATH, this.hierarchyService, this.repliconService);
        o.generateExcel();
    }
}
