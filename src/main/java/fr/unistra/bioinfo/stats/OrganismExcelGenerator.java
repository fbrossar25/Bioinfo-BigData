package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.common.EventUtils;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.io.FileUtils;
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
        try {
            FileUtils.forceMkdirParent(f);
        } catch (IOException e) {
            LOGGER.error("Erreur lors des la création du dossier {}", f.getParentFile().getAbsolutePath());
            return false;
        }
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
        String path = base_path + File.separator;

        switch (level) {
            case KINGDOM:
                path += o.getKingdom() + ".xlsx";
                break;
            case GROUP:
                path += o.getKingdom();
                path += File.separator + o.getGroup() + ".xlsx";
                break;
            case SUB_GROUP:
                path += o.getKingdom(); 
                path += File.separator + o.getGroup();
                path += File.separator + o.getSubgroup() + ".xlsx";
                break;
            case ORGANISM:
                path += o.getKingdom();
                path += File.separator + o.getGroup();
                path += File.separator + o.getSubgroup();
                path += File.separator + o.getOrganism() + ".xlsx";
                break;
        }

        return path;
    }

    public boolean generate_excel_organism(List<RepliconEntity> replicons) {
        XSSFWorkbook wb = new XSSFWorkbook();

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
            LOGGER.trace("SUB GROUP ORGA EMPTY");
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
                LOGGER.trace("NEW Sheet for " + t);
            }
            LOGGER.trace("" + t);
        }


        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.SUB_GROUP));

        return true;
    }

    public boolean genetate_excel_group() {
        try(XSSFWorkbook wb = new XSSFWorkbook()){
            List<HierarchyEntity> orgas = this.hierarchyService.getByGroup(this.organism.getGroup());
            if (orgas.isEmpty()) {
                LOGGER.trace("GROUP ORGA EMPTY");
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
                    LOGGER.trace("NEW Sheet for " + t);
                }
                LOGGER.debug("" + t);
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
                LOGGER.trace("KINDOM ORGA EMPTY");
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
                    LOGGER.trace("NEW Sheet for " + t);
                }
                LOGGER.trace("" + t);
            }

            write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.KINGDOM));

        }catch(IOException e){
            LOGGER.error("Erreur lors de l'écriture de l'organisme '{}'", this.organism.getOrganism(), e);
        }
        return true;
    }

    public boolean generateExcel() {
        List<RepliconEntity> replicons = repliconService.getByHierarchy(this.organism);
        if (this.generate_excel_organism(replicons)) {
            LOGGER.warn("OK ORGA {}", this.organism.getOrganism());
            for(RepliconEntity r : replicons){
                EventUtils.sendEvent( EventUtils.EventType.STATS_END_REPLICON,r);
            }
            EventUtils.sendEvent( EventUtils.EventType.STATS_END_ORGANISM, this.organism.getOrganism());

        } else {
            LOGGER.trace("FAIL ORGA");
            return false;
        }


        if (this.genetate_excel_sub_group()) {
            LOGGER.warn("OK SS GR {}", this.organism.getSubgroup());
            EventUtils.sendEvent( EventUtils.EventType.STATS_END_SUBGROUP, this.organism.getSubgroup());
        } else {
            LOGGER.trace("FAIL SS GR");
            return false;
        }


        if (this.genetate_excel_group()) {
            LOGGER.warn("OK GROUP {}", this.organism.getGroup());
            EventUtils.sendEvent( EventUtils.EventType.STATS_END_GROUP, this.organism.getGroup());
        } else {
            LOGGER.trace("FAIL SS GROUP");
            return false;
        }


        if (this.generate_excel_kingdom()) {
            LOGGER.warn("OK KINGDOM {}", this.organism.getGroup());
            EventUtils.sendEvent( EventUtils.EventType.STATS_END_KINGDOM, this.organism.getKingdom());
        } else {
            LOGGER.trace("FAIL KINGDOM");
            return false;
        }

        for (RepliconEntity r : replicons) {
            r.setComputed(true);
        }
        this.repliconService.saveAll(replicons);

        EventUtils.sendEvent( EventUtils.EventType.STATS_END);
        return true;
    }
}
