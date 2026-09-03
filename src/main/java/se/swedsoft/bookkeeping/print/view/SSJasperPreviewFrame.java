package se.swedsoft.bookkeeping.print.view;


import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.view.save.JRRtfSaveContributor;
import se.swedsoft.bookkeeping.gui.SSMainFrame;
import se.swedsoft.bookkeeping.gui.status.SSStatusBar;
import se.swedsoft.bookkeeping.gui.util.SSBundle;
import se.swedsoft.bookkeeping.gui.util.components.SSButton;
import se.swedsoft.bookkeeping.gui.util.filechooser.SSJasperFileChooser;
import se.swedsoft.bookkeeping.gui.util.filechooser.util.SSFilterHTM;
import se.swedsoft.bookkeeping.gui.util.filechooser.util.SSFilterPDF;
import se.swedsoft.bookkeeping.gui.util.filechooser.util.SSFilterRTF;
import se.swedsoft.bookkeeping.gui.util.filechooser.util.SSFilterXLSX;
import se.swedsoft.bookkeeping.gui.util.frame.SSDefaultTableFrame;
import se.swedsoft.bookkeeping.print.SSReport;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * $Id$
 *
 */
public class SSJasperPreviewFrame extends SSDefaultTableFrame implements PropertyChangeListener {    private static final Logger LOG = LoggerFactory.getLogger(SSJasperPreviewFrame.class);


    public static ResourceBundle bundle = SSBundle.getBundle();

    private JasperPrint iPrinter;

    private SSViewer iViewer;

    private JLabel iPageLabel;

    private JComboBox<SSZoomLevel> iZoomLevels;

    private SSReport iReport;
    private Runnable iOnOutputAction;
    private Runnable iOnEmailAction;
    private boolean iShowEmailButton;
    private boolean iOutputActionHandled;
    private SSButton iEmailButton;

    SSButton iFirst;
    SSButton iBack;
    SSButton iForward;
    SSButton iLast;

    /**
     * Default constructor.
     * @param frame owning main frame
     * @param width frame width
     * @param height frame height
     */
    public SSJasperPreviewFrame(SSMainFrame frame, int width, int height) {
        super(frame, SSBundle.getBundle().getString("printpreviewframe.title"), width,
                height);

        iViewer.addPropertyChangeListener("page_zoom", evt -> updateStatusBar());

        iViewer.addPropertyChangeListener("page_change", evt -> updateStatusBar());

    }

    /**
     * This method should return a toolbar if the sub-class wants one.
     * Otherwise, it may return null.
     *
     * @return A JToolBar or null.
     */
    @Override
    public JToolBar getToolBar() {
        JToolBar toolbar = new JToolBar();

        iZoomLevels = new JComboBox<>(SSZoomLevel.values());
        iZoomLevels.setEditable(true);
        iZoomLevels.setMaximumSize(new Dimension(75, 20));
        iZoomLevels.setSelectedItem(SSZoomLevel.ZOOM_100);

        // Save
        // ***************************
        SSButton iButton = new SSButton("ICON_SAVEITEM", "printpreviewframe.savebutton",
                e -> {

                        SSJasperFileChooser iFileChooser = SSJasperFileChooser.getInstance();

                        if (iReport != null) {
                            String iTitle = (String) iReport.getParameter("title");

                            iFileChooser.setSelectedFile(new File(iTitle + ".pdf"));
                        } else {
                            iFileChooser.setSelectedFile(new File("Rapport.pdf"));

                        }

                        if (iFileChooser.showSaveDialog(SSJasperPreviewFrame.this)
                                == JFileChooser.APPROVE_OPTION) {
                            fireOutputAction();
                            saveDocument(iFileChooser.getFileFilter(),
                                    iFileChooser.getSelectedFile());
                        }


                    });

        iButton.setDefaultSize();
        toolbar.add(iButton);

        // Email
        // ***************************
        iEmailButton = new SSButton("ICON_Email", "printpreviewframe.emailbutton",
                e -> {
                        if (iOnEmailAction == null) {
                            return;
                        }
                        iOnEmailAction.run();
                   });
        iEmailButton.setDefaultSize();
        iEmailButton.setVisible(iShowEmailButton);
        iEmailButton.setEnabled(iOnEmailAction != null);
        toolbar.add(iEmailButton);

        // Print
        // ***************************
        iButton = new SSButton("ICON_PRINT", "printpreviewframe.printbutton",
                e -> {
                        fireOutputAction();

                        try {
                            JasperPrintManager.printReport(iPrinter, true);
                        } catch (JRException ex) {
                            LOG.error("Unexpected error", ex);
                        }

                    });
        iButton.setDefaultSize();
        toolbar.add(iButton);
        toolbar.addSeparator();

        // First
        // ***************************
        iFirst = new SSButton("ICON_FIRST", "printpreviewframe.firstbutton",
                e -> iViewer.firstPage());
        iFirst.setDefaultSize();
        toolbar.add(iFirst);

        // Back
        // ***************************
        iBack = new SSButton("ICON_BACK", "printpreviewframe.prevbutton",
                e -> iViewer.prevPage());
        iBack.setDefaultSize();
        toolbar.add(iBack);

        // Forward
        // ***************************
        iForward = new SSButton("ICON_FORWARD", "printpreviewframe.nextbutton",
                e -> iViewer.nextPage());
        iForward.setDefaultSize();
        toolbar.add(iForward);

        // Last
        // ***************************
        iLast = new SSButton("ICON_LAST", "printpreviewframe.lastbutton",
                e -> iViewer.lastPage());
        iLast.setDefaultSize();
        toolbar.add(iLast);
        toolbar.addSeparator();

        // Zoom in
        // ***************************
        iButton = new SSButton("ICON_ZOOMIN", "printpreviewframe.zoominbutton",
                e -> {

                        int index = iZoomLevels.getSelectedIndex() - 1;

                        if (index >= 0) {
                            iZoomLevels.setSelectedItem(SSZoomLevel.values()[index]);
                        }


                    });
        iButton.setDefaultSize();
        toolbar.add(iButton);

        // Zoom out
        // ***************************
        iButton = new SSButton("ICON_ZOOMOUT", "printpreviewframe.zoomoutbutton",
                e -> {

                        int index = iZoomLevels.getSelectedIndex() + 2;

                        if (index < SSZoomLevel.values().length) {
                            iZoomLevels.setSelectedItem(SSZoomLevel.values()[index - 1]);
                        }


                    });
        iButton.setDefaultSize();
        toolbar.add(iButton);
        toolbar.addSeparator();

        iZoomLevels.addActionListener(
                e -> {


                        int iZoom;

                        try {
                            if (iZoomLevels.getSelectedIndex() >= 0) {
                                Object iSelected = iZoomLevels.getSelectedItem();
                                if (!(iSelected instanceof SSZoomLevel)) {
                                    return;
                                }
                                iZoom = ((SSZoomLevel) iSelected).getZoom();
                            } else {
                                Object iSelected = iZoomLevels.getSelectedItem();
                                if (iSelected == null) {
                                    return;
                                }
                                iZoom = Integer.parseInt(iSelected.toString());
                            }

                        } catch (NumberFormatException e1) {
                            return;
                        }
                        iViewer.setZoom(iZoom);


                    });

        toolbar.add(iZoomLevels);

        iViewer.addPropertyChangeListener("page_change", this);

        return toolbar;
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */

    public void propertyChange(PropertyChangeEvent evt) {
        int iCount = iViewer.getPageCount();
        int iCurrent = iViewer.getCurrentPage();

        iFirst.setEnabled(iCurrent > 0);
        iBack.setEnabled(iCurrent > 0);
        iForward.setEnabled(iCurrent < (iCount - 1));
        iLast.setEnabled(iCurrent < (iCount - 1));
    }

    /**
     * This method should return the main content for the frame.
     * Such as an object table.
     *
     * @return The main content for this frame.
     */
    @Override
    public JComponent getMainContent() {
        iViewer = new SSViewer();

        JScrollPane iScrollPane = new JScrollPane(iViewer);

        iScrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        iScrollPane.setVerticalScrollBarPolicy(
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        return iScrollPane;
    }

    /**
     * This method should return the status bar content, if any.
     *
     * @return The content for the status bar or null if none is wanted.
     */
    @Override
    public JComponent getStatusBar() {
        iPageLabel = new JLabel();

        SSStatusBar iStatusBar = new SSStatusBar();

        iStatusBar.addSpacer();
        iStatusBar.addPanel(iPageLabel);

        return iStatusBar;
    }

    /**
     * Indicates whether this frame is a company data related frame.
     *
     * @return A boolean value.
     */
    @Override
    public boolean isCompanyFrame() {
        return true;
    }

    /**
     * Indicates whether this frame is a year data related frame.
     *
     * @return A boolean value.
     */
    @Override
    public boolean isYearDataFrame() {
        return true;
    }

    /**
     * Sets the rendered Jasper print in the viewer.
     *
     * @param iPrinter print object to display
     */
    public void setPrinter(JasperPrint iPrinter) {
        this.iPrinter = iPrinter;

        iViewer.setPrinter(iPrinter);
    }

    /**
     * Stores the source report metadata used during save.
     *
     * @param iReport report descriptor
     */
    public void setReport(SSReport iReport) {
        this.iReport = iReport;
    }

    /**
     * Sets an action callback that is executed on first save/print action.
     *
     * @param iOnOutputAction callback to invoke when output action is performed
     */
    public void setOnOutputAction(Runnable iOnOutputAction) {
        this.iOnOutputAction = iOnOutputAction;
        iOutputActionHandled = false;
    }

    /**
     * Sets the action callback for e-mail action in preview toolbar.
     *
     * @param iOnEmailAction callback to invoke for e-mail action
     */
    public void setOnEmailAction(Runnable iOnEmailAction) {
        this.iOnEmailAction = iOnEmailAction;
        if (iEmailButton != null) {
            iEmailButton.setVisible(iShowEmailButton);
            iEmailButton.setEnabled(iOnEmailAction != null);
        }
    }

    /**
     * Controls whether the e-mail button should be visible in the preview toolbar.
     *
     * @param iShowEmailButton true if the button should be visible
     */
    public void setShowEmailButton(boolean iShowEmailButton) {
        this.iShowEmailButton = iShowEmailButton;
        if (iEmailButton != null) {
            iEmailButton.setVisible(iShowEmailButton);
            iEmailButton.setEnabled(iOnEmailAction != null);
        }
    }

    /**
     *
     */
    private void updateStatusBar() {
        int iCurrent = iViewer.getCurrentPage() + 1;
        int iTotal = iViewer.getPageCount();

        // iStatusBar.setText( String.format(bundle.getString("printpreviewframe.pages"), iCurrent,iTotal )   );
        iPageLabel.setText(
                String.format(bundle.getString("printpreviewframe.pages"), iCurrent,
                iTotal));
    }

    /**
     * Saves the current report using the selected format.
     *
     * @param pFileFilter active file filter that decides output format
     * @param pSelectedFile destination file selected by user
     */
    private void saveDocument(FileFilter pFileFilter, File pSelectedFile) {
        String iFileName = pSelectedFile.getAbsolutePath();
        String iFileExt = getExtension(pSelectedFile);

        // Pdf
        if (iFileExt.equals("pdf")
                || (iFileExt.isEmpty() && pFileFilter instanceof SSFilterPDF)) {
            try {
                if (iFileExt.isEmpty()) {
                    iFileName = iFileName + ".pdf";
                }

                JasperExportManager.exportReportToPdfFile(iPrinter, iFileName);
            } catch (JRException ex) {
                LOG.error("Unexpected error", ex);
            }
        }
        // html
        if (iFileExt.equals("htm") || iFileExt.equals("html")
                || (iFileExt.isEmpty() && pFileFilter instanceof SSFilterHTM)) {
            try {
                if (iFileExt.isEmpty()) {
                    iFileName = iFileName + ".htm";
                }

                JasperExportManager.exportReportToHtmlFile(iPrinter, iFileName);
            } catch (JRException ex) {
                LOG.error("Unexpected error", ex);
            }
        }

        // RTF
        if (iFileExt.equals(".rtf")
                || (iFileExt.isEmpty() && pFileFilter instanceof SSFilterRTF)) {
            try {
                JRRtfSaveContributor iSaver = new JRRtfSaveContributor(Locale.forLanguageTag("sv-SE"), bundle);

                iSaver.save(iPrinter, pSelectedFile);
            } catch (JRException ex) {
                LOG.error("Unexpected error", ex);
            }
        }

        // Excel
        if (iFileExt.equals("xlsx")
                || (iFileExt.isEmpty() && pFileFilter instanceof SSFilterXLSX)) {

            try {
                if (iFileExt.isEmpty()) {
                    iFileName = iFileName + ".xlsx";
                }

                JRXlsxExporter iExporter = new JRXlsxExporter();
                iExporter.setExporterInput(new SimpleExporterInput(iPrinter));
                iExporter.setExporterOutput(new SimpleOutputStreamExporterOutput(iFileName));
                iExporter.exportReport();
            } catch (JRException ex) {
                LOG.error("Unexpected error", ex);
            }
        }

    }

    private void fireOutputAction() {
        if (iOutputActionHandled || iOnOutputAction == null) {
            return;
        }
        iOutputActionHandled = true;
        iOnOutputAction.run();
    }

    /*
     * Get the lowercase extension of a file.
     */
    private String getExtension(File pFile) {
        String ext = null;
        String s = pFile.getName();

        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }

        return ext == null ? "" : ext.toLowerCase();
    }

    @Override
    public String toString() {
        return "se.swedsoft.bookkeeping.print.view.SSJasperPreviewFrame"
                + "{iBack=" + iBack
                + ", iFirst=" + iFirst
                + ", iForward=" + iForward
                + ", iLast=" + iLast
                + ", iPageLabel=" + iPageLabel
                + ", iPrinter=" + iPrinter
                + ", iReport=" + iReport
                + ", iViewer=" + iViewer
                + ", iZoomLevels=" + iZoomLevels
                + '}';
    }
}
