package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import org.apache.poi.ss.usermodel.Workbook;
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




    public Boolean generate_excel_organism()
    {
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
