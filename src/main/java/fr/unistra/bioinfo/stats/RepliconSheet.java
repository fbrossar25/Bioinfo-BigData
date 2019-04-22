package fr.unistra.bioinfo.stats;

import fr.unistra.bioinfo.persistence.entity.Phase;
import fr.unistra.bioinfo.persistence.entity.RepliconEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.util.*;

public class RepliconSheet {
    private static final List<String> HEADERS = Arrays.asList(
            "Trinucleotide",
            "Phase 0",
            "Phase 1",
            "Phase 2",
            "freq. phase 0",
            "freq. phase 1",
            "freq. phase 2",
            "phase pref 0",
            "phase pref 1",
            "phase pref 2",
            "",
            "Dinucleotide",
            "Phase 0",
            "Phase 1",
            "freq. phase 0",
            "freq. phase 1",
            "phase pref 0",
            "phase pref 1"
    );

    protected List<RepliconEntity> replicons = null;
    protected XSSFWorkbook wb = null;
    protected GeneralInformationSheet.LEVEL level;
    private XSSFSheet sheet = null;
    private Map<String, CellStyle> styles = new HashMap<String, CellStyle>();

    private void generate_styles() {
        this.styles.put("header", generate_header_style(this.wb));
        this.styles.put("nuc", generate_nucleotide_style(this.wb));
        this.styles.put("even num", generate_even_number_style(this.wb));
        this.styles.put("odd num", generate_odd_number_style(this.wb));
        this.styles.put("even perc", generate_even_percent_style(this.wb));
        this.styles.put("odd perc", generate_odd_percent_style(this.wb));
        this.styles.put("total", generate_total_style(this.wb));
        this.styles.put("perc total", generate_percent_total_style(this.wb));
    }

    private static CellStyle generate_header_style(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();

        XSSFFont font = wb.createFont();
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

    private static CellStyle generate_nucleotide_style(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();

        XSSFFont font = wb.createFont();
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

    private static CellStyle generate_even_number_style(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(207, 227, 247)));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderBottom(BorderStyle.DASHED);
        style.setBorderTop(BorderStyle.DASHED);
        style.setBorderLeft(BorderStyle.DASHED);
        style.setBorderRight(BorderStyle.DASHED);

        return style;
    }

    private static CellStyle generate_odd_number_style(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.DASHED);
        style.setBorderTop(BorderStyle.DASHED);
        style.setBorderLeft(BorderStyle.DASHED);
        style.setBorderRight(BorderStyle.DASHED);

        return style;
    }

    private static CellStyle generate_even_percent_style(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(207, 227, 247)));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderBottom(BorderStyle.DASHED);
        style.setBorderTop(BorderStyle.DASHED);
        style.setBorderLeft(BorderStyle.DASHED);
        style.setBorderRight(BorderStyle.DASHED);

        style.setDataFormat(wb.createDataFormat().getFormat("0.0000"));

        return style;
    }

    private static CellStyle generate_odd_percent_style(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.DASHED);
        style.setBorderTop(BorderStyle.DASHED);
        style.setBorderLeft(BorderStyle.DASHED);
        style.setBorderRight(BorderStyle.DASHED);
        style.setDataFormat(wb.createDataFormat().getFormat("0.0000"));

        return style;
    }

    private static CellStyle generate_total_style(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();

        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 15);
        font.setColor(new XSSFColor(new java.awt.Color(242, 242, 242)));
        font.setBold(true);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(75, 86, 96)));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setFont(font);

        return style;
    }

    private static CellStyle generate_percent_total_style(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();

        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 15);
        font.setColor(new XSSFColor(new java.awt.Color(242, 242, 242)));
        font.setBold(true);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(75, 86, 96)));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(wb.createDataFormat().getFormat("0.0000"));
        style.setFont(font);

        return style;
    }

    public RepliconSheet(XSSFWorkbook wb, List<RepliconEntity> replicons, GeneralInformationSheet.LEVEL level) {
        this.wb = wb;
        this.replicons = replicons;
        this.level = level;

        this.sheet = this.wb.createSheet(this.determine_sheet_name());
        this.generate_styles();
    }

    public RepliconSheet(XSSFWorkbook wb, RepliconEntity replicon) {
        this.wb = wb;
        this.replicons = Arrays.asList(replicon);
        this.level = GeneralInformationSheet.LEVEL.ORGANISM;

        this.sheet = this.wb.createSheet(this.determine_sheet_name());
        this.generate_styles();
    }

    private String determine_sheet_name() {
        RepliconEntity r = this.replicons.get(0);
        String s = r.getType().toString().toLowerCase();
        if (this.level == GeneralInformationSheet.LEVEL.ORGANISM) {
            return "" + s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() + "_" + r.getName() + "." + r.getVersion();
        } else {

            return "Sum_" + s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
        }
    }

    private void write_header() {
        Row row = this.sheet.createRow(0);
        Cell cell = null;
        for (int i = 0; i < HEADERS.size(); i++) {
            if (HEADERS.get(i) != "") {
                String header = HEADERS.get(i);
                cell = row.createCell(i);
                cell.setCellValue(header);
                cell.setCellStyle(this.styles.get("header"));

            }
        }
    }

    private void write_trinucleotides_headers() {
        String tri = null;
        Row row = null;
        Cell cell = null;
        int row_number = 1;

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                for (char ccc : "ACGT".toCharArray()) {
                    tri = "" + c + cc + ccc;
                    row = this.sheet.createRow(row_number);
                    cell = row.createCell(0);
                    cell.setCellValue(tri.toUpperCase());
                    cell.setCellStyle(this.styles.get("nuc"));
                    row_number++;
                }
            }
        }
        row = this.sheet.createRow(row_number);
        cell = row.createCell(0);
        cell.setCellValue("Total");
        cell.setCellStyle(this.styles.get("total"));
    }

    private void write_dinucleotides_headers() {
        String din = null;
        Row row = null;
        Cell cell = null;
        int row_number = 1;

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                din = "" + c + cc;
                row = this.sheet.getRow(row_number);
                cell = row.createCell(11);
                cell.setCellValue(din.toUpperCase());
                cell.setCellStyle(this.styles.get("nuc"));
                row_number++;
            }
        }
        row = this.sheet.getRow(row_number);
        cell = row.createCell(11);
        cell.setCellValue("Total");
        cell.setCellStyle(this.styles.get("total"));
    }

    private void write_tri(RepliconEntity r, Phase ph) {
        String tri = null;
        Row row = null;
        Cell cell = null;
        int row_pos = 1 + ph.ordinal();
        int row_number = 1;

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                for (char ccc : "ACGT".toCharArray()) {
                    tri = ("" + c + cc + ccc).toUpperCase();
                    row = this.sheet.getRow(row_number);
                    cell = row.createCell(row_pos);
                    cell.setCellValue(r.getTrinucleotideCount(tri, ph));
                    if (row_number % 2 == 0) {
                        cell.setCellStyle(this.styles.get("even num"));
                    } else {
                        cell.setCellStyle(this.styles.get("odd num"));
                    }
                    row_number++;
                }
            }
        }
    }


    private void write_din(RepliconEntity r, Phase ph) {
        String din = null;
        Row row = null;
        Cell cell = null;
        int row_pos = 12 + ph.ordinal();
        int row_number = 1;

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                din = ("" + c + cc).toUpperCase();
                row = this.sheet.getRow(row_number);
                cell = row.createCell(row_pos);
                cell.setCellValue(r.getDinucleotideCount(din, ph));
                if (row_number % 2 == 0) {
                    cell.setCellStyle(this.styles.get("even num"));
                } else {
                    cell.setCellStyle(this.styles.get("odd num"));
                }
                row_number++;
            }
        }
    }

    private void write_total(RepliconEntity r, List<Integer> total, Boolean is_trin) {
        Row row = null;
        Cell cell = null;
        int trin_total_pos = 64 + 1;
        int din_total_pos = 16 + 1;

        if (is_trin) {
            for (int i = 0; i < 3; i++) {
                row = this.sheet.getRow(trin_total_pos);
                cell = row.createCell(1 + i);
                cell.setCellValue(total.get(i));
                cell.setCellStyle(this.styles.get("total"));
            }
        } else {
            for (int i = 0; i < 2; i++) {
                row = this.sheet.getRow(din_total_pos);
                cell = row.createCell(12 + i);
                cell.setCellValue(total.get(i));
                cell.setCellStyle(this.styles.get("total"));
            }
        }
    }

    private double getFrequency(int n, int total) {
        if ( total == 0 || n == 0 ) {
            return 0.0;
        } else {
            return (((double)n) / ((double)total));
        }
    }
    private void write_freq_trin(RepliconEntity r, List<Integer> total) {
        String tri = "";
        Row row = null;
        Cell cell = null;
        Integer col_num = 4;
        Integer row_number = 1;
        List<Double> total_sum = new ArrayList<Double>();
        total_sum.add(0.0);
        total_sum.add(0.0);
        total_sum.add(0.0);

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                for (char ccc : "ACGT".toCharArray()) {
                    tri = ("" + c + cc + ccc).toUpperCase();
                    row = this.sheet.getRow(row_number);

                    for (Phase ph : Phase.values()) {
                        double freq = 0.0;
                        switch (ph) {
                            case PHASE_0:
                                freq = this.getFrequency(r.getTrinucleotideCount(tri, Phase.PHASE_0),total.get(0));
                                total_sum.set(0, total_sum.get(0) + freq);
                                break;
                            case PHASE_1:
                                freq = this.getFrequency(r.getTrinucleotideCount(tri, Phase.PHASE_1),total.get(1));
                                total_sum.set(1, total_sum.get(1) + freq);
                                break;
                            case PHASE_2:
                                freq = this.getFrequency(r.getTrinucleotideCount(tri, Phase.PHASE_2),total.get(2));
                                total_sum.set(2, total_sum.get(2) + freq);
                                break;
                        }

                        cell = row.createCell(col_num + ph.ordinal());
                        cell.setCellType(CellType.NUMERIC);
                        cell.setCellValue(freq);
                        if (row_number % 2 == 0) {
                            cell.setCellStyle(this.styles.get("even perc"));
                        } else {
                            cell.setCellStyle(this.styles.get("odd perc"));
                        }
                    }

                    row_number++;
                }
            }
        }

        row = this.sheet.getRow(64 + 1);

        for (Phase ph : Phase.values()) {
            double n = 0.1337;
            switch (ph) {
                case PHASE_0:
                    n = total_sum.get(0);
                    break;
                case PHASE_1:
                    n = total_sum.get(1);
                    break;
                case PHASE_2:
                    n = total_sum.get(2);
                    break;
            }

            cell = row.createCell(col_num + ph.ordinal());
            cell.setCellType(CellType.NUMERIC);
            cell.setCellValue(n);
            cell.setCellStyle(this.styles.get("perc total"));
        }


    }

    private void write_freq_din(RepliconEntity r, List<Integer> total) {
        String value = "";
        String din = "";
        Row row = null;
        Cell cell = null;
        Integer col_num = 14;
        Integer row_number = 1;
        List<Double> sum_total = new ArrayList<Double>();
        sum_total.add(0.0);
        sum_total.add(0.0);

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                din = ("" + c + cc).toUpperCase();
                row = this.sheet.getRow(row_number);

                for (int ph = 0; ph < 2; ph++) {
                    double freq = 0.0;
                    switch (ph) {
                        case 0:
                            freq = getFrequency(r.getDinucleotideCount(din, Phase.PHASE_0), total.get(0));
                            sum_total.set(0, sum_total.get(0) + freq);
                            break;
                        case 1:
                            freq = getFrequency(r.getDinucleotideCount(din, Phase.PHASE_1), total.get(1));
                            sum_total.set(1, sum_total.get(1) + freq);
                            break;
                    }

                    cell = row.createCell(col_num + ph);
                    cell.setCellType(CellType.NUMERIC);
                    cell.setCellValue(freq);

                    if (row_number % 2 == 0) {
                        cell.setCellStyle(this.styles.get("even perc"));
                    } else {
                        cell.setCellStyle(this.styles.get("odd perc"));
                    }
                }

                row_number++;
            }
        }

        row = this.sheet.getRow(16 + 1);

        for (int ph = 0; ph < 2; ph++) {
            cell = row.createCell(col_num + ph);
            cell.setCellValue(sum_total.get(ph));
            cell.setCellStyle(this.styles.get("perc total"));
        }


    }

    private void write_pref_trin(RepliconEntity r, Phase ph) {
        String tri = null;
        Row row = null;
        Cell cell = null;
        Integer col_num = 7 + ph.ordinal();
        Integer row_number = 1;

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                for (char ccc : "ACGT".toCharArray()) {
                    tri = ("" + c + cc + ccc).toUpperCase();
                    row = this.sheet.getRow(row_number);
                    cell = row.createCell(col_num);
                    cell.setCellValue(r.getPhasePrefTrinucleotide(tri, ph));

                    if (row_number % 2 == 0) {
                        cell.setCellStyle(this.styles.get("even num"));
                    } else {
                        cell.setCellStyle(this.styles.get("odd num"));
                    }

                    row_number++;
                }
            }
        }
    }

    private void write_pref_din(RepliconEntity r, Phase ph) {
        String din = null;
        Row row = null;
        Cell cell = null;
        Integer col_num = 16 + ph.ordinal();
        Integer row_number = 1;

        for (char c : "ACGT".toCharArray()) {
            for (char cc : "ACGT".toCharArray()) {
                din = ("" + c + cc).toUpperCase();
                row = this.sheet.getRow(row_number);
                cell = row.createCell(col_num);
                cell.setCellValue(r.getPhasePrefDinucleotide(din, ph));

                if (row_number % 2 == 0) {
                    cell.setCellStyle(this.styles.get("even num"));
                } else {
                    cell.setCellStyle(this.styles.get("odd num"));
                }

                row_number++;
            }
        }
    }

    public void write_sheet() {
        RepliconEntity r = null;
        if (this.level == GeneralInformationSheet.LEVEL.ORGANISM) {
            r = this.replicons.get(0);
        } else {
            r = RepliconEntity.add(this.replicons);
        }

        List<Integer> total_trin = new ArrayList<Integer>();
        total_trin.add(r.getTotalTrinucleotides(Phase.PHASE_0));
        total_trin.add(r.getTotalTrinucleotides(Phase.PHASE_1));
        total_trin.add(r.getTotalTrinucleotides(Phase.PHASE_2));

        List<Integer> total_din = new ArrayList<Integer>();
        total_din.add(r.getTotalDinucleotides(Phase.PHASE_0));
        total_din.add(r.getTotalDinucleotides(Phase.PHASE_1));

        this.write_header();
        this.write_trinucleotides_headers();
        this.write_dinucleotides_headers();

        this.write_tri(r, Phase.PHASE_0);
        this.write_tri(r, Phase.PHASE_1);
        this.write_tri(r, Phase.PHASE_2);

        this.write_din(r, Phase.PHASE_0);
        this.write_din(r, Phase.PHASE_1);

        this.write_total(r, total_trin, true);
        this.write_total(r, total_din, false);

        this.write_freq_trin(r, total_trin);
        this.write_freq_din(r, total_din);

        this.write_pref_trin(r, Phase.PHASE_0);
        this.write_pref_trin(r, Phase.PHASE_1);
        this.write_pref_trin(r, Phase.PHASE_2);

        this.write_pref_din(r, Phase.PHASE_0);
        this.write_pref_din(r, Phase.PHASE_1);

        for (int i = 0; i < HEADERS.size(); i++) {
            if (i != 10) {
                this.sheet.autoSizeColumn(i);
            }
        }
    }
}
