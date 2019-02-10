package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconType;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

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

    private static final String SHEET_NAME = "General Information";

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

    private Map<String, XSSFCellStyle> styles = new HashMap<>();

    private void generate_styles()
    {
        this.styles.put("main", this.generate_main_style());
        this.styles.put("value", this.generate_value_style());
    }

    private XSSFCellStyle generate_main_style ()
    {
        XSSFCellStyle style = this.wb.createCellStyle();

        XSSFFont font = this.wb.createFont();
        font.setFontHeightInPoints((short) 15);
        font.setColor(new XSSFColor(new java.awt.Color(242, 242, 242)));
        font.setBold(true);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(75, 86, 96)));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setFont(font);

        return style;
    }
    private XSSFCellStyle generate_value_style ()
    {
        XSSFCellStyle style = this.wb.createCellStyle();

        XSSFFont font = this.wb.createFont();
        font.setFontHeightInPoints((short) 15);
        font.setColor(new XSSFColor(new java.awt.Color(242, 242, 242)));
        font.setBold(false);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(106, 119, 132)));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setFont(font);

        return style;
    }

    public GeneralInformationSheet (XSSFWorkbook wb, List<HierarchyEntity> organisms, List<RepliconEntity> replicons, GeneralInformationSheet.LEVEL level)
    {
        this.wb = wb;
        this.replicons = replicons;
        this.organisms = organisms;
        this.sheet = this.wb.createSheet( SHEET_NAME );
        this.level = level;
        this.generate_styles();
    }

    public GeneralInformationSheet (XSSFWorkbook wb, HierarchyEntity organism, List<RepliconEntity> replicons, GeneralInformationSheet.LEVEL level)
    {
        this.wb = wb;
        this.replicons = replicons;
        this.organisms = Arrays.asList( organism );
        this.sheet = this.wb.createSheet( SHEET_NAME );
        this.level = level;
        this.generate_styles();
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
            if ( FIRST_COLNAMES.get(i) != "" )
            {
                cell.setCellStyle(this.styles.get("main"));
            }

            cell = row.createCell(3);
            cell.setCellValue(SECOND_COLNAMES.get(i));
            if ( SECOND_COLNAMES.get(i) != "" )
            {
                cell.setCellStyle(this.styles.get("main"));
            }

        }

        // Write organism and Date
        // TODO: gestion des levels

        switch ( this.level )
        {
            case ORGANISM:
                row = this.sheet.getRow(0);
                cell = row.createCell(1);
                cell.setCellValue(this.organisms.get(0).getOrganism());
                cell.setCellStyle(this.styles.get("value"));
            case SUB_GROUP:
                row = this.sheet.getRow(1);
                cell = row.createCell(1);
                cell.setCellValue(this.organisms.get(0).getSubgroup());
                cell.setCellStyle(this.styles.get("value"));
            case GROUP:
                row = this.sheet.getRow(2);
                cell = row.createCell(1);
                cell.setCellValue(this.organisms.get(0).getGroup());
                cell.setCellStyle(this.styles.get("value"));
            case KINGDOM:
                row = this.sheet.getRow(3);
                cell = row.createCell(1);
                cell.setCellValue(this.organisms.get(0).getKingdom());
                cell.setCellStyle(this.styles.get("value"));
                break;
        }

        Integer nb_valid_cds = 0;
        Integer nb_invalid_cds = 0;
        for ( RepliconEntity r : this.replicons )
        {
            nb_invalid_cds += r.getInvalidsCDS();
            nb_valid_cds += r.getValidsCDS();
        }

        row = this.sheet.getRow(5);
        cell = row.createCell(1);
        cell.setCellValue(nb_valid_cds);
        cell.setCellStyle(this.styles.get("value"));

        row = this.sheet.getRow(6);
        cell =row.createCell(1);
        cell.setCellValue(nb_invalid_cds);
        cell.setCellStyle(this.styles.get("value"));

        row = this.sheet.getRow(7);
        cell = row.createCell(1);
        cell.setCellValue(this.organisms.size());
        cell.setCellStyle(this.styles.get("value"));

        row = this.sheet.getRow(0);
        cell = row.createCell(4);
        cell.setCellValue(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) );
        cell.setCellStyle(this.styles.get("value"));

        // TYPE
        HashMap<RepliconType, Integer> types_count = this.CDS_types_count();

        row = this.sheet.getRow(4);
        cell = row.createCell(4);
        cell.setCellValue(types_count.get(RepliconType.MITOCHONDRION));
        cell.setCellStyle(this.styles.get("value"));

        row = this.sheet.getRow(5);
        cell = row.createCell(4);
        cell.setCellValue(types_count.get(RepliconType.PLAST));
        cell.setCellStyle(this.styles.get("value"));

        row = this.sheet.getRow(6);
        cell = row.createCell(4);
        cell.setCellValue(types_count.get(RepliconType.PLASMID));
        cell.setCellStyle(this.styles.get("value"));

        row = this.sheet.getRow(7);
        cell = row.createCell(4);
        cell.setCellValue(types_count.get(RepliconType.DNA));
        cell.setCellStyle(this.styles.get("value"));

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
