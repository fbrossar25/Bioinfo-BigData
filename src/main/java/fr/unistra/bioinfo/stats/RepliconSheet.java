package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Arrays;
import java.util.List;

public class RepliconSheet {
    protected List<RepliconEntity> replicons = null;
    protected XSSFWorkbook wb = null;
    protected GeneralInformationSheet.LEVEL level;
    private XSSFSheet sheet = null;

    public RepliconSheet (XSSFWorkbook wb, List<RepliconEntity> replicons, GeneralInformationSheet.LEVEL level)
    {
        this.wb = wb;
        this.replicons = replicons;
        this.level = level;

        this.sheet = this.wb.createSheet(this.determine_sheet_name());
    }

    public RepliconSheet (XSSFWorkbook wb, RepliconEntity replicon)
    {
        this.wb = wb;
        this.replicons = Arrays.asList(replicon);
        this.level = GeneralInformationSheet.LEVEL.ORGANISM;

        this.sheet = this.wb.createSheet(this.determine_sheet_name());
    }

    private String determine_sheet_name()
    {
        if ( this.level == GeneralInformationSheet.LEVEL.ORGANISM )
        {
            RepliconEntity r = this.replicons.get(0);
            return "" + r.getType().toString().toLowerCase() + "_" + r.getName() + "." + r.getVersion();
        }
        else
        {
            return this.level.toString().toLowerCase();
        }
    }

    public void write_sheet()
    {
    }
}
