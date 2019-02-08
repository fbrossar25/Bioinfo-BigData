package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;

public class GeneralInformationSheet {
    public static enum LEVEL
    {
        ORGANISM,
        SUB_GROUP,
        GROUP,
        KINGDOM
    }

    private static final String SHEET_NAME = "general_information";

    private static final List<String> FIRST_COLNAMES = Arrays.asList(
            "Organism",
            "Sub-group",
            "Group",
            "Kindom",
            "",
            "# of valid CDS",
            "# of invalid CDS",
            "# of organism"
            );


    private static final List<String> SECOND_COLNAMES = Arrays.asList(
            "Last update",
            "",
            "",
            "",
            "# Mitochondion",
            "# Plast",
            "# Plasmid",
            "# DNA"
    );

    protected XSSFWorkbook wb = null;
    protected XSSFSheet sheet = null;
    protected List<RepliconEntity> replicons = null;
    protected List<HierarchyEntity> organisms = null;
    protected LEVEL level = null;

    public GeneralInformationSheet (XSSFWorkbook wb, List<HierarchyEntity> organisms, List<RepliconEntity> replicons, GeneralInformationSheet.LEVEL level)
    {
        this.wb = wb;
        this.replicons = replicons;
        this.organisms = organisms;
        this.sheet = this.wb.createSheet( SHEET_NAME );
        this.level = level;
    }

    public GeneralInformationSheet (XSSFWorkbook wb, HierarchyEntity organism, List<RepliconEntity> replicons, GeneralInformationSheet.LEVEL level)
    {
        this.wb = wb;
        this.replicons = replicons;
        this.organisms = Arrays.asList( organism );
        this.sheet = this.wb.createSheet( SHEET_NAME );
        this.level = level;
    }

    public void write_lines ()
    {
        Row row = null;
        Cell cell = null;
//        CellStyle basic_style = this.wb.createCellStyle();
//
//        basic_style.setBorderTop(BorderStyle.THICK);
//        basic_style.setBorderRight(BorderStyle.THIN);
//        basic_style.setBorderBottom(BorderStyle.THIN);
//        basic_style.setBorderLeft(BorderStyle.THIN);

        for ( int i = 0 ; i < FIRST_COLNAMES.size() ; i++ )
        {
            row = this.sheet.createRow(i);
            cell = row.createCell(0);
            cell.setCellValue(FIRST_COLNAMES.get(i));

            cell = row.createCell(3);
            cell.setCellValue(SECOND_COLNAMES.get(i));
        }

        // Write organism and Date
        // TODO: gestion des levels

        switch ( this.level )
        {
            case ORGANISM:
                row = this.sheet.getRow(0);
                cell = row.createCell(1);
                cell.setCellValue(this.organisms.get(0).getOrganism());
            case SUB_GROUP:
                row = this.sheet.getRow(1);
                cell = row.createCell(1);
                cell.setCellValue(this.organisms.get(0).getSubgroup());
            case GROUP:
                row = this.sheet.getRow(2);
                cell = row.createCell(1);
                cell.setCellValue(this.organisms.get(0).getGroup());
            case KINGDOM:
                row = this.sheet.getRow(3);
                cell = row.createCell(1);
                cell.setCellValue(this.organisms.get(0).getKingdom());
                break;
        }

        row = this.sheet.getRow(5);
        cell = row.createCell(1);
        cell.setCellValue("NC");

        row = this.sheet.getRow(6);
        cell =row.createCell(1);
        cell.setCellValue("NC");

        row = this.sheet.getRow(7);
        cell = row.createCell(1);
        cell.setCellValue(this.organisms.size());

        row = this.sheet.getRow(0);
        cell = row.createCell(4);
        cell.setCellValue(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) );

        // TYPE
        HashMap<RepliconType, Integer> types_count = this.CDS_types_count();

        row = this.sheet.getRow(4);
        cell = row.createCell(4);
        cell.setCellValue(types_count.get(RepliconType.MITOCHONDRION));

        row = this.sheet.getRow(5);
        cell = row.createCell(4);
        cell.setCellValue(types_count.get(RepliconType.PLAST));

        row = this.sheet.getRow(6);
        cell = row.createCell(4);
        cell.setCellValue(types_count.get(RepliconType.PLASMID));

        row = this.sheet.getRow(7);
        cell = row.createCell(4);
        cell.setCellValue(types_count.get(RepliconType.DNA));


        // reshape cells
        this.sheet.autoSizeColumn(0);
        this.sheet.autoSizeColumn(1);
        this.sheet.autoSizeColumn(3);
        this.sheet.autoSizeColumn(4);
    }

    public HashMap<RepliconType, Integer> CDS_types_count ()
    {
        HashMap<RepliconType, Integer> h = new HashMap<>();

        for ( RepliconType t : RepliconType.values() )
        {
            h.put(t, this.CDS_type_count(t));
        }

        return h;
    }

    public Integer CDS_type_count (RepliconType t)
    {
        int n = 0;
        for ( RepliconEntity replicon : this.replicons ) {
            if ( replicon.getType() == t )
            {
                n++;
            }
        }

        return n;
    }

}
