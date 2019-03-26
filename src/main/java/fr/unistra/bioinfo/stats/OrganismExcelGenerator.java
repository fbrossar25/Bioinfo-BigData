package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrganismExcelGenerator {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private HierarchyEntity organism = null;
    private String base_path = null;

    private HierarchyService hierarchyService;
    private RepliconService repliconService;

    @Autowired
    public OrganismExcelGenerator(HierarchyEntity organism, String base_path, HierarchyService hierarchyService, RepliconService repliconService) {
        this.organism = organism;
        this.base_path = base_path;
        this.hierarchyService = hierarchyService;
        this.repliconService = repliconService;
    }

    public OrganismExcelGenerator(HierarchyEntity organism, HierarchyService hierarchyService, RepliconService repliconService) {
        this(organism, "Results", hierarchyService, repliconService);
    }

    public static boolean write_workbook(Workbook wb, String path) {
        File f = new File(path);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            wb.write(fos);
            wb.close();
        } catch (IOException e) {
            LOGGER.error("Erreur lors de l'écriture du fichier '{}'", path, e);
            return false;
        }

        return true;
    }

    public static String generate_path(HierarchyEntity o, String base_path, GeneralInformationSheet.LEVEL level) {
        String path = base_path + "/";

        switch (level) {
            case KINGDOM:
                path += o.getKingdom() + ".xlsx";
                break;
            case GROUP:
                path += o.getKingdom();
                path += "/" + o.getGroup() + ".xlsx";
                break;
            case SUB_GROUP:
                path += o.getKingdom();
                path += "/" + o.getGroup();
                path += "/" + o.getSubgroup() + ".xlsx";
                break;
            case ORGANISM:
                path += o.getKingdom();
                path += "/" + o.getGroup();
                path += "/" + o.getSubgroup();
                path += "/" + o.getOrganism() + ".xlsx";
                break;
        }

        return path;
    }

    public static boolean generate_herarchy_dir(HierarchyEntity o, String base_path) {
        String path = base_path;
        path += File.separator + o.getKingdom();
        path += File.separator + o.getGroup();
        path += File.separator + o.getSubgroup();
        return new File(path).mkdirs();
    }

    public boolean generate_excel_organism() {
        //Ligne a décommenter pour les tests.
        /*
        if (!OrganismExcelGenerator.generate_herarchy_dir(this.organism, this.base_path)) {
            return false;
        }*/

        XSSFWorkbook wb = new XSSFWorkbook();
        List<RepliconEntity> replicons = repliconService.getByHierarchy(this.organism);


        new GeneralInformationSheet(wb, this.organism, replicons).write_lines();

        for (RepliconEntity r : replicons) {
            new RepliconSheet(wb, r).write_sheet();
        }

        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.ORGANISM));
        return true;
    }


    private List<RepliconEntity> getListRepliconsByType(List<RepliconEntity> repls, RepliconType t) {
        List<RepliconEntity> results = new ArrayList<>();

        for (RepliconEntity r : repls) {
            if (r.getType().equals(t)) {
                results.add(r);
            }
        }

        return results;
    }

    public boolean genetate_excel_sub_group() {
        XSSFWorkbook wb = new XSSFWorkbook();

        List<HierarchyEntity> orgas = this.hierarchyService.getBySubgroup(this.organism.getSubgroup());
        if (orgas.isEmpty()) {
            LOGGER.info("SUB GROUP ORGA EMPTY");
            return false;
        }


        List<RepliconEntity> repls = new ArrayList<>();
        for (HierarchyEntity o : orgas) {
            List<RepliconEntity> tmp_repls = repliconService.getByHierarchy(o);
            repls.addAll(tmp_repls);
        }

        new GeneralInformationSheet(wb, orgas, repls, GeneralInformationSheet.LEVEL.SUB_GROUP).write_lines();

        for (RepliconType t : RepliconType.values()) {
            List<RepliconEntity> typedRepls = this.getListRepliconsByType(repls, t);
            if (!typedRepls.isEmpty()) {
                new RepliconSheet(wb, typedRepls, GeneralInformationSheet.LEVEL.SUB_GROUP).write_sheet();
                LOGGER.info("NEW Sheet for " + t);
            }
            LOGGER.info("" + t);
        }


        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.SUB_GROUP));
        return true;
    }

    public boolean genetate_excel_group() {
        try(XSSFWorkbook wb = new XSSFWorkbook()){
            List<HierarchyEntity> orgas = this.hierarchyService.getByGroup(this.organism.getGroup());
            if (orgas.isEmpty()) {
                LOGGER.info("GROUP ORGA EMPTY");
                return false;
            }

            List<RepliconEntity> repls = new ArrayList<>();
            for (HierarchyEntity o : orgas) {
                List<RepliconEntity> tmp_repls = repliconService.getByHierarchy(o);
                repls.addAll(tmp_repls);
            }

            new GeneralInformationSheet(wb, orgas, repls, GeneralInformationSheet.LEVEL.GROUP).write_lines();

            for (RepliconType t : RepliconType.values()) {
                List<RepliconEntity> typedRepls = this.getListRepliconsByType(repls, t);
                if (!typedRepls.isEmpty()) {
                    new RepliconSheet(wb, typedRepls, GeneralInformationSheet.LEVEL.GROUP).write_sheet();
                    LOGGER.info("NEW Sheet for " + t);
                }
                LOGGER.info("" + t);
            }

            write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.GROUP));
        }catch(IOException e){
            LOGGER.error("Erreur lors de l'écriture de l'organisme '{}'", this.organism.getOrganism(), e);
        }
        return true;
    }

    public boolean generate_excel_kingdom() {
        try(XSSFWorkbook wb = new XSSFWorkbook()){
            List<HierarchyEntity> orgas = this.hierarchyService.getByKingdom(this.organism.getKingdom());
            if (orgas.isEmpty()) {
                LOGGER.info("KINDOM ORGA EMPTY");
                return false;
            }

            List<RepliconEntity> repls = new ArrayList<>();
            for (HierarchyEntity o : orgas) {
                List<RepliconEntity> tmp_repls = repliconService.getByHierarchy(o);
                repls.addAll(tmp_repls);
            }

            new GeneralInformationSheet(wb, orgas, repls, GeneralInformationSheet.LEVEL.KINGDOM).write_lines();

            for (RepliconType t : RepliconType.values()) {
                List<RepliconEntity> typedRepls = this.getListRepliconsByType(repls, t);
                if (!typedRepls.isEmpty()) {
                    new RepliconSheet(wb, typedRepls, GeneralInformationSheet.LEVEL.KINGDOM).write_sheet();
                    LOGGER.info("NEW Sheet for " + t);
                }
                LOGGER.info("" + t);
            }

            write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.KINGDOM));
        }catch(IOException e){
            LOGGER.error("Erreur lors de l'écriture de l'organisme '{}'", this.organism.getOrganism(), e);
        }
        return true;
    }

    public boolean generateExcel() {
        if (this.generate_excel_organism()) {
            LOGGER.info("OK ORGA");
        } else {
            LOGGER.info("FAIL ORGA");
            return false;
        }


        if (this.genetate_excel_sub_group()) {
            LOGGER.info("OK SS GR");
        } else {
            LOGGER.info("FAIL SS GR");
            return false;
        }


        if (this.genetate_excel_group()) {
            LOGGER.info("OK GROUP");
        } else {
            LOGGER.info("FAIL SS GROUP");
            return false;
        }


        if (this.generate_excel_kingdom()) {
            LOGGER.info("OK KINGDOM");
        } else {
            LOGGER.info("FAIL KINGDOM");
            return false;
        }

        for (RepliconEntity r : this.repliconService.getByHierarchy(this.organism)) {
            r.setComputed(true);
            this.repliconService.save(r);
        }

        return true;
    }
}
