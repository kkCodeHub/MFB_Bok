package se.swedsoft.bookkeeping.importexport.sie.util;


import java.io.*;
import java.util.LinkedList;
import java.util.List;


/**
 * Date: 2006-feb-20
 * Time: 12:59:15
 */
public class SIEFile {
    private SIEFile() {}

    /**
     *
     * @param pFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static List<String> readFile(File pFile) throws FileNotFoundException, IOException {
        List<String> iLines = new LinkedList<>();

        try (BufferedReader iReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(pFile), "IBM-437"))) {
            String iLine;

            while ((iLine = iReader.readLine()) != null) {
                iLines.add(iLine);
            }
        }
        return iLines;

    }

    /**
     *
     * @param pFile
     * @param iLines
     * @throws IOException
     */
    public static void writeFile(File pFile, List<String> iLines) throws IOException {
        try (BufferedWriter iWriter = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(pFile), "IBM-437"))) {
            for (String iLine: iLines) {
                iWriter.write(iLine);
                iWriter.newLine();
            }
        }
    }
}
