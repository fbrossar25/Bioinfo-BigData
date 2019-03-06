package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.Main;
import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
import fr.unistra.bioinfo.persistence.service.HierarchyService;
import fr.unistra.bioinfo.persistence.service.RepliconService;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.jdbc.Work;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganismExcelGenerator
{
    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private HierarchyEntity organism = null;
    private String base_path = null;

    @Autowired
    private HierarchyService hierarchyService;
    @Autowired
    private RepliconService repliconService;

    public OrganismExcelGenerator (HierarchyEntity organism, String base_path, HierarchyService hierarchyService, RepliconService repliconService)
    {
        this.organism = organism;
        this.base_path = base_path;
        this.hierarchyService = hierarchyService;
        this.repliconService = repliconService;
    }
    public OrganismExcelGenerator (HierarchyEntity organism, HierarchyService hierarchyService, RepliconService repliconService)
    {
        this(organism, "Results", hierarchyService, repliconService);
    }

    public static Boolean write_workbook (Workbook wb, String path)
    {
        File f = new File(path);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
            wb.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return false;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static String generate_path(HierarchyEntity o, String base_path,  GeneralInformationSheet.LEVEL level)
    {
        String path = base_path + "/";

        switch ( level )
        {
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

    public static Boolean generate_herarchy_dir ( HierarchyEntity o, String base_path )
    {
        StringBuilder path = new StringBuilder(base_path);

        /*try
        {

            FileUtils.forceMkdir(new File(path.toString()));

            path.append(File.separator + o.getKingdom());
            FileUtils.forceMkdir(new File(path.toString()));

            path.append(File.separator + o.getGroup());
            FileUtils.forceMkdir(new File(path.toString()));

            path.append(File.separator + o.getSubgroup());
            FileUtils.forceMkdir(new File(path.toString()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        */

        return true;
    }

    public Boolean generate_excel_organism()
    {
        if ( ! OrganismExcelGenerator.generate_herarchy_dir(this.organism, this.base_path) )
        {
            return false;
        }

        XSSFWorkbook wb = new XSSFWorkbook();
        List<RepliconEntity> replicons = repliconService.getByHierarchy(this.organism);


        new GeneralInformationSheet(wb, this.organism, replicons).write_lines();

        for ( RepliconEntity r : replicons )
        {
            new RepliconSheet(wb, r).write_sheet();
        }

        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.ORGANISM));
        return true;
    }


    private List<RepliconEntity> getListRepliconsByType(List<RepliconEntity> repls, RepliconType t )
    {
        List<RepliconEntity> results = new ArrayList<>();

        for ( RepliconEntity r : repls )
        {
            if ( r.getType().equals(t) )
            {
                results.add(r);
            }
        }

        return results;
    }

    public Boolean genetate_excel_sub_group ()
    {
        XSSFWorkbook wb = new XSSFWorkbook();

        List<HierarchyEntity> orgas = this.hierarchyService.getBySubgroup(this.organism.getSubgroup());
        List<RepliconEntity> repls = new ArrayList<RepliconEntity>();
        for ( HierarchyEntity o : orgas )
        {
            List<RepliconEntity> tmp_repls = repliconService.getByHierarchy(o);
            repls.addAll(tmp_repls);
        }

        new GeneralInformationSheet(wb, orgas, repls, GeneralInformationSheet.LEVEL.SUB_GROUP).write_lines();

        for ( RepliconType t : RepliconType.values() )
        {
            List<RepliconEntity> typedRepls = this.getListRepliconsByType(repls, t);
            if ( !typedRepls.isEmpty() )
            {
                new RepliconSheet(wb, typedRepls, GeneralInformationSheet.LEVEL.SUB_GROUP).write_sheet();
                LOGGER.info("NEW Sheet for " + t);
            }
            LOGGER.info("" + t);
        }


        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.SUB_GROUP));
        return true;
    }

    public  Boolean genetate_excel_group ()
    {
        XSSFWorkbook wb = new XSSFWorkbook();

        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.GROUP));
        return true;
    }

    public Boolean generate_excel_kingdom ()
    {
        XSSFWorkbook wb = new XSSFWorkbook();

        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.KINGDOM));
        return true;
    }

    public Boolean generateExcel()
    {
        if ( this.generate_excel_organism() )
        {
            LOGGER.info("OK ORGA");
        }
        else
        {
            LOGGER.info("FAIL ORGA");
            return false;
        }


        if ( this.genetate_excel_sub_group() )
        {
            LOGGER.info("OK SS GR");
        }
        else
        {
            LOGGER.info("FAIL SS GR");
            return false;
        }


        if ( this.genetate_excel_group() )
        {
            LOGGER.info("OK GROUP");
        }
        else
        {
            LOGGER.info("FAIL SS GROUP");
            return false;
        }


        if ( this.generate_excel_kingdom() )
        {
            LOGGER.info("OK KINGDOM");
        }
        else
        {
            LOGGER.info("FAIL KINGDOM");
            return false;
        }

        return true;
    }
}
