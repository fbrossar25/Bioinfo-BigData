package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.CommonUtils;
import fr.unistra.bioinfo.configuration.StaticInitializer;
import fr.unistra.bioinfo.genbank.GenbankUtils;
import fr.unistra.bioinfo.parsing.GenbankParser;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.io.FileUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({StaticInitializer.class})
@SpringBootTest
@TestPropertySource(locations = {"classpath:application-test.properties"})
public class ExcelTest {
    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static String TEST_PATH = "test_results";
    public static final String ORGA_NAME = "Felis catus";
    private static final Path GENBANK_TEST_FILE_PATH = Paths.get(".","src", "test", "resources", "NC_001700.1.gb");
    private static final Path GENBANK_TEST_FILE_PATH_2 = Paths.get(".","src", "test", "resources", "NC_001701.1.gb");
    private static final Path GENBANK_TEST_FILE_PATH_3 = Paths.get(".","src", "test", "resources", "NC_001702.1.gb");

    @Autowired
    private HierarchyService hierarchyService;
    @Autowired
    private RepliconService repliconService;

    private HierarchyEntity orga = null;
    private List<RepliconEntity> repls = null;

    @BeforeEach
    void beforeEach(){
        GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile());
        GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH_2.toFile());
        GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH_3.toFile());

        this.orga = hierarchyService.getByOrganism(ORGA_NAME);
        this.repls = repliconService.getByHierarchy(this.orga);

        try {
            FileUtils.forceMkdir(new File(TEST_PATH));
        } catch (IOException e) {
            fail(e);
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
        String replace = "\\".equals(File.separator) ? "\\\\" : File.separator;
        assertEquals(
                OrganismExcelGenerator.generate_path(this.orga, TEST_PATH, GeneralInformationSheet.LEVEL.ORGANISM),
                "test_results/Eukaryota/Animals/Mammals/Felis catus.xlsx".replaceAll("/", replace)
        );
        assertEquals(
                OrganismExcelGenerator.generate_path(this.orga, TEST_PATH, GeneralInformationSheet.LEVEL.SUB_GROUP),
                "test_results/Eukaryota/Animals/Mammals.xlsx".replaceAll("/", replace)
        );
        assertEquals(
                OrganismExcelGenerator.generate_path(this.orga, TEST_PATH, GeneralInformationSheet.LEVEL.GROUP),
                "test_results/Eukaryota/Animals.xlsx".replaceAll("/", replace)
        );
        assertEquals(
                OrganismExcelGenerator.generate_path(this.orga, TEST_PATH, GeneralInformationSheet.LEVEL.KINGDOM),
                "test_results/Eukaryota.xlsx".replaceAll("/", replace)
        );
    }

    @Test
    void test_excel_organism_write()
    {
        OrganismExcelGenerator o = new OrganismExcelGenerator(this.orga, TEST_PATH, this.hierarchyService, this.repliconService);
        o.generate_excel_organism(repliconService.getByHierarchy(this.orga));
    }

    @Test
    @Disabled
    void testShouldGenerateExcelForFelisCatus(){
        FileUtils.deleteQuietly(Paths.get(TEST_PATH).toFile());
        afterEach();
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile()));
        RepliconEntity r = this.repliconService.getByName("NC_001700");
        assertNotNull(r);
        HierarchyEntity h = r.getHierarchyEntity();
        assertNotNull(h);
        assertEquals("Felis catus", h.getOrganism());
        assertTrue(new OrganismExcelGenerator(h, TEST_PATH, this.hierarchyService, this.repliconService).generateExcel());
    }

    @Test
    @Disabled
    void testShouldGenerateExcelForDifferentOrganism(){
        FileUtils.deleteQuietly(Paths.get(TEST_PATH).toFile());
        afterEach();
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH.toFile()));
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH_2.toFile()));
        assertTrue(GenbankParser.parseGenbankFile(GENBANK_TEST_FILE_PATH_3.toFile()));
        RepliconEntity r = this.repliconService.getByName("NC_001700");
        assertNotNull(r);
        HierarchyEntity h = r.getHierarchyEntity();
        assertNotNull(h);
        assertEquals("Felis catus", h.getOrganism());
        assertTrue(new OrganismExcelGenerator(h, TEST_PATH, this.hierarchyService, this.repliconService).generateExcel());
        r = this.repliconService.getByName("NC_001702");
        assertNotNull(r);
        h = r.getHierarchyEntity();
        assertNotNull(h);
        assertEquals("Bos taurus", h.getOrganism());
        assertTrue(new OrganismExcelGenerator(h, TEST_PATH, this.hierarchyService, this.repliconService).generateExcel());
    }

    @Test
    void test_generateExcel()
    {
        assertEquals(3, this.repliconService.count().longValue());
        OrganismExcelGenerator o = new OrganismExcelGenerator(this.orga, TEST_PATH, this.hierarchyService, this.repliconService);
        o.generateExcel();

        OrganismExcelGenerator oo = new OrganismExcelGenerator( hierarchyService.getByOrganism("Bos taurus"), TEST_PATH, this.hierarchyService, this.repliconService);
        oo.generateExcel();
    }

    @Test
    void parseAndGenerateStats(){
        GenbankUtils.updateNCDatabase(1);
        CompletableFuture<List<RepliconEntity>> future = new CompletableFuture<>();
        long begin = System.currentTimeMillis();
        GenbankUtils.downloadAllReplicons(future);
        List<RepliconEntity> files = null;
        try {
            files = future.get(1, TimeUnit.HOURS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            fail("Erreur lors du téléchargement", e);
        }
        assertNotNull(files, "Pas de résultats renvoyé");
        assertFalse(files.isEmpty(), "Pas de fichier trouvés");

        long end = System.currentTimeMillis();
        LOGGER.info("Elapsed time for downloading and parsing {} replicons: {}s", repliconService.count(), LocalTime.MIN.plus(end-begin, ChronoUnit.MILLIS).toString());

        begin = System.currentTimeMillis();
        for(HierarchyEntity entity : hierarchyService.getAll()){
            new OrganismExcelGenerator(entity, TEST_PATH, this.hierarchyService, this.repliconService).generateExcel();
        }
        end = System.currentTimeMillis();
        LOGGER.info("Elapsed time for generating excels for {} organisms: {}s", hierarchyService.count(), LocalTime.MIN.plus(end-begin, ChronoUnit.MILLIS).toString());
    }

    @Test
    void generateStats(){
        GenbankUtils.updateNCDatabase(100);
        File dataDir = CommonUtils.DATAS_PATH.toFile();
        File[] files = dataDir.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0, "Pas de fichier trouvés");

        long begin = System.currentTimeMillis();
        for(HierarchyEntity entity : hierarchyService.getAll()){
            new OrganismExcelGenerator(entity, TEST_PATH, this.hierarchyService, this.repliconService).generateExcel();
        }
        long end = System.currentTimeMillis();
        long elapsedSeconds = (end - begin) / 1000;
        LOGGER.info("Elapsed time for generating excels for {} organisms: {}s", hierarchyService.count(), elapsedSeconds);
        long prediction = (long)((((double) elapsedSeconds) / hierarchyService.count()) * 4000);
        LOGGER.info("Generating all organism's Excels (~5000 organisms) should take less than {}", LocalTime.MIN.plusSeconds(prediction).toString());
    }

    @Test
    @Disabled
    void generateExcelWithBadOrganismNames(){
        HierarchyEntity h = new HierarchyEntity();
        h.setOrganism("oragnism:*/<>.\\|");
        h.setGroup("group:*/<>./\\");
        h.setSubgroup("subgroup:");
        h.setKingdom("kingdom:");
        hierarchyService.save(h);
        RepliconEntity r = new RepliconEntity("NC_012345", 1, h);
        repliconService.save(r);
        try{
            new OrganismExcelGenerator(h, TEST_PATH, this.hierarchyService, this.repliconService).generateExcel();
        }catch (Throwable e){
            fail(e);
        }
    }
}
