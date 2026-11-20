package se.swedsoft.bookkeeping.print.util;


import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleGraphics2DExporterOutput;
import net.sf.jasperreports.export.SimpleGraphics2DReportConfiguration;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

/**
 * Prints a {@link JasperPrint} with a job name chosen by the application, instead of the
 * "JasperReports - name" job name used by JasperPrintManager.
 */
public final class SSPrintJob {

    private SSPrintJob() {}

    /**
     * Shows the print dialog and prints all pages.
     *
     * @param pPrint the filled report
     * @param pJobName print job name, also suggested as file name by PDF printers
     * @return true if the user confirmed the print dialog and the document was printed,
     *         false if the dialog was cancelled (no printing took place)
     * @throws JRException if printing fails
     */
    public static boolean print(JasperPrint pPrint, String pJobName) throws JRException {
        PrinterJob iJob = PrinterJob.getPrinterJob();

        iJob.setJobName(pJobName);

        PageFormat iFormat = iJob.defaultPage();
        Paper iPaper = new Paper();
        double iWidth = pPrint.getPageWidth();
        double iHeight = pPrint.getPageHeight();

        if (iWidth > iHeight) {
            iPaper.setSize(iHeight, iWidth);
            iPaper.setImageableArea(0, 0, iHeight, iWidth);
            iFormat.setOrientation(PageFormat.LANDSCAPE);
        } else {
            iPaper.setSize(iWidth, iHeight);
            iPaper.setImageableArea(0, 0, iWidth, iHeight);
            iFormat.setOrientation(PageFormat.PORTRAIT);
        }
        iFormat.setPaper(iPaper);

        iJob.setPrintable(new JasperPrintable(pPrint), iFormat);

        if (iJob.printDialog()) {
            try {
                iJob.print();
            } catch (PrinterException e) {
                throw new JRException(e);
            }
            return true;
        }

        return false;
    }

    private static final class JasperPrintable implements Printable {

        private final JasperPrint iPrint;

        private JasperPrintable(JasperPrint pPrint) {
            iPrint = pPrint;
        }

        @Override
        public int print(Graphics pGraphics, PageFormat pFormat, int pPageIndex) throws PrinterException {
            if (pPageIndex < 0 || pPageIndex >= iPrint.getPages().size()) {
                return NO_SUCH_PAGE;
            }
            Graphics2D iGraphics = (Graphics2D) pGraphics.create();

            try {
                iGraphics.translate(pFormat.getImageableX(), pFormat.getImageableY());

                float iZoom = (float) Math.min(pFormat.getImageableWidth() / iPrint.getPageWidth(),
                        pFormat.getImageableHeight() / iPrint.getPageHeight());

                JRGraphics2DExporter iExporter = new JRGraphics2DExporter(DefaultJasperReportsContext.getInstance());
                SimpleGraphics2DExporterOutput iOutput = new SimpleGraphics2DExporterOutput();
                SimpleGraphics2DReportConfiguration iConfiguration = new SimpleGraphics2DReportConfiguration();

                iOutput.setGraphics2D(iGraphics);
                iConfiguration.setPageIndex(pPageIndex);
                iConfiguration.setZoomRatio(iZoom);

                iExporter.setExporterInput(new SimpleExporterInput(iPrint));
                iExporter.setExporterOutput(iOutput);
                iExporter.setConfiguration(iConfiguration);
                iExporter.exportReport();
            } catch (JRException e) {
                throw new PrinterException(e.getMessage());
            } finally {
                iGraphics.dispose();
            }
            return PAGE_EXISTS;
        }
    }
}
