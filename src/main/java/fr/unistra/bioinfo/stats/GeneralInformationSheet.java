package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.persistence.entity.HierarchyEntity;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
}
