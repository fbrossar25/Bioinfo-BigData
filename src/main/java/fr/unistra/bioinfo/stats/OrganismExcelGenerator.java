package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.jdbc.Work;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class OrganismExcelGenerator {
    private HierarchyEntity organism = null;
    private String base_path = null;

    public OrganismExcelGenerator (HierarchyEntity organism, String base_path)
    {
        this.organism = organism;
        this.base_path = base_path;
    }
    public OrganismExcelGenerator (HierarchyEntity organism)
    {
        this(organism, "Results");
    }

    public static void write_workbook (Workbook wb, String path)
    {
        File f = new File(path);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            wb.write(fos);
            wb.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public Boolean generate_excel_organism()
    {
        Workbook wb = new XSSFWorkbook();

        write_workbook(wb, generate_path(this.organism, this.base_path, GeneralInformationSheet.LEVEL.ORGANISM));
        return true;
    }

    public Boolean genetate_excel_sub_group ()
    {
        return true;
    }

    public  Boolean genetate_excel_group ()
    {
        return true;
    }

    public Boolean generate_excel_kingdom ()
    {
        return true;
    }

    public Boolean generateExcel()
    {
        return true;
    }
}
