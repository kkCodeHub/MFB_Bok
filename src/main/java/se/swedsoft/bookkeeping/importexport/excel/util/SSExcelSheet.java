package se.swedsoft.bookkeeping.importexport.excel.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2006-feb-14
 * Time: 11:30:33
 */
public class SSExcelSheet {

    private Sheet iSheet;

    /**
     *
     * @param pSheet
     */
    public SSExcelSheet(Sheet pSheet) {
        iSheet = pSheet;
    }

    /**
     *
     * @return
     */
    public List<SSExcelRow> getRows() {
        List<SSExcelRow> iList = new LinkedList<>();

        for (int i = 0; i <= iSheet.getLastRowNum(); i++) {
            Row iRow_POI = iSheet.getRow(i);
            iList.add(new SSExcelRow(iRow_POI, i));
        }
        return iList;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.importexport.excel.util.SSExcelSheet");
        sb.append("{iSheet=").append(iSheet);
        sb.append('}');
        return sb.toString();
    }
}
