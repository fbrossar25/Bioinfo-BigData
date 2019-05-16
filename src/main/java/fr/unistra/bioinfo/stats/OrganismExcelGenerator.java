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
import java.util.stream.Collectors;

public class OrganismExcelGenerator {
    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private HierarchyEntity organism = null;
    private String base_path = null;

    private HierarchyService hierarchyService;
    private RepliconService repliconService;

    private static final Object synchronizedObject = new Object();

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
        StringBuilder pathBuilder = new StringBuilder(base_path.length() * 2);
        pathBuilder.append(base_path.replaceAll("[\\\\<>:\"/|?*]", "")).append(File.separator);

        switch (level) {
            case KINGDOM:
                pathBuilder.append(o.getKingdom().replaceAll("[\\\\<>:\"/|?*]", ""));
                break;
            case GROUP:
                pathBuilder.append(o.getKingdom().replaceAll("[\\\\<>:\"/|?*]", ""));
                pathBuilder.append(File.separator).append(o.getGroup().replaceAll("[\\\\<>:\"/|?*]", ""));
                break;
            case SUB_GROUP:
                pathBuilder.append(o.getKingdom().replaceAll("[\\\\<>:\"/|?*]", ""));
                pathBuilder.append(File.separator).append(o.getGroup().replaceAll("[\\\\<>:\"/|?*]", ""));
                pathBuilder.append(File.separator).append(o.getSubgroup().replaceAll("[\\\\<>:\"/|?*]", ""));
                break;
            case ORGANISM:
                pathBuilder.append(o.getKingdom().replaceAll("[\\\\<>:\"/|?*]", ""));
                pathBuilder.append(File.separator).append(o.getGroup().replaceAll("[\\\\<>:\"/|?*]", ""));
                pathBuilder.append(File.separator).append(o.getSubgroup().replaceAll("[\\\\<>:\"/|?*]", ""));
                pathBuilder.append(File.separator).append(o.getOrganism().replaceAll("[\\\\<>:\"/|?*]", ""));
                break;
        }

        return pathBuilder.append(".xlsx").toString();
    }

    public boolean generate_excel_organism(List<RepliconEntity> replicons) {
        XSSFWorkbook wb = new XSSFWorkbook();

        new GeneralInformationSheet(wb, this.organism, replicons).write_lines();

        // RepliconSheet(XSSFWorkbook wb, List<RepliconEntity> replicons, GeneralInformationSheet.LEVEL level)
//        new RepliconSheet(wb, replicons, GeneralInformationSheet.LEVEL.SUB_GROUP).write_sheet();

        // sum of replicons by types
        for (RepliconType t : RepliconType.values()) {
            List<RepliconEntity> typedRepls = this.getListRepliconsByType(replicons, t);
            if (!typedRepls.isEmpty()) {
                new RepliconSheet(wb, typedRepls, GeneralInformationSheet.LEVEL.SUB_GROUP).write_sheet();
                LOGGER.trace("NEW Sheet for " + t);
            }
            LOGGER.trace("" + t);
        }

        // replicons sheets
        for (RepliconEntity r : replicons) {
            new RepliconSheet(wb, r).write_sheet();
        }

        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.ORGANISM));


        return true;
    }


    /**
     * Write every
     * @return
     */
    private List<RepliconEntity> getListRepliconsByType(List<RepliconEntity> repls, final RepliconType t) {
        return repls.stream().filter(r -> r.getType().equals(t)).collect(Collectors.toList());
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

    /**
     * Write all Excels (orga, ss group, group, kingdom)
     * @return
     */
    public boolean generateExcel() {

        // Get parsed replicon list and write organism
        List<RepliconEntity> replicons = repliconService.getByHierarchy(this.organism)
                .stream().filter(RepliconEntity::isParsed).collect(Collectors.toList());
        this.write_organism(replicons);

        // Set replicon to computed
        this.setOrganismComputed(replicons);

        // Write sub excel
        if ( !this.writeSubExcel())
        {
            return false;
        }
        EventUtils.sendEvent( EventUtils.EventType.STATS_END);
        return true;
    }

    /**
     * Write organism only
     * @return
     */
    public boolean generateExcelOrganismOnly() {
        List<RepliconEntity> replicons = repliconService.getByHierarchy(this.organism);

        // Get replicon list and write organism
        this.write_organism(replicons);

        // Set replicon to computed
        this.setOrganismComputed(replicons);

        EventUtils.sendEvent( EventUtils.EventType.STATS_END);
        return true;
    }

    /**
     * Set all organism replicons to COMPUTED
     * @param replicons
     */
    private void setOrganismComputed( List<RepliconEntity> replicons ) {
        // Set replicon to computed
        for (RepliconEntity r : replicons) {
            r.setComputed(true);
        }
        synchronized (synchronizedObject){
            this.repliconService.saveAll(replicons);
        }
    }

    /**
     * Write Excel organism
     * @param replicons
     * @return
     */
    public boolean write_organism( List<RepliconEntity> replicons ) {
        if (this.generate_excel_organism(replicons)) {
            LOGGER.debug("Organisme '{}' mis à jour", this.organism.getOrganism());

            for(RepliconEntity r : replicons){
                if(r.isParsed()) {
                    EventUtils.sendEvent(EventUtils.EventType.STATS_END_REPLICON, r);
                }
            }

            EventUtils.sendEvent( EventUtils.EventType.STATS_END_ORGANISM, this.organism.getOrganism());
            return true;
        } else {
            LOGGER.warn("Échec mise à jour organisme '{}'", this.organism.getOrganism());
            return false;
        }
    }


    public boolean writeSubExcel() {

        // write wub group
        if ( !this.repliconService.hasRepliconToProceedForSubgroup(this.organism.getSubgroup()) ) {
            if (this.genetate_excel_sub_group()) {
                LOGGER.info("Sous-groupe '{}' mis à jour", this.organism.getSubgroup());
                EventUtils.sendEvent( EventUtils.EventType.STATS_END_SUBGROUP, this.organism.getSubgroup());
            } else {
                LOGGER.warn("Échec mise à jour sous-groupe '{}'", this.organism.getSubgroup());
                return false;
            }
//            LOGGER.warn("MAJ SUB GROUP '{}'", this.organism.getSubgroup());
        }

        // write group
        if ( !this.repliconService.hasRepliconToProceedForGroup(this.organism.getGroup()) ) {
            if (this.genetate_excel_group()) {
                LOGGER.info("Groupe '{}' mis à jour", this.organism.getGroup());
                EventUtils.sendEvent( EventUtils.EventType.STATS_END_GROUP, this.organism.getGroup());
            } else {
                LOGGER.warn("Échec mise à jour groupe '{}'", this.organism.getGroup());
                return false;
            }
//            LOGGER.warn("MAJ GROUP '{}'", this.organism.getGroup());
        }

        // Write kingdom
        if ( !this.repliconService.hasRepliconToProceedForKingdom(this.organism.getKingdom()) ) {
            if (this.generate_excel_kingdom()) {
                LOGGER.info("Royaume '{}' mis à jour", this.organism.getKingdom());
                EventUtils.sendEvent( EventUtils.EventType.STATS_END_KINGDOM, this.organism.getKingdom());
            } else {
                LOGGER.warn("Échec mise à jour royaume '{}'", this.organism.getKingdom());
                return false;
            }
//            LOGGER.warn("MAJ KINGDOM '{}'", this.organism.getKingdom());
        }

        return true;
    }
}
