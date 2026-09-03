package se.swedsoft.bookkeeping.gui.util.filechooser;


import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;

import se.swedsoft.bookkeeping.gui.util.filechooser.util.SSFilterXLS;
import se.swedsoft.bookkeeping.gui.util.filechooser.util.SSFilterXLSX;


/**
 * Date: 2006-feb-13
 * Time: 14:47:02
 */
public class SSExcelFileChooser extends SSFileChooser {

    private static SSExcelFileChooser cInstance;
    private final SSFilterXLS iXlsFilter;
    private final SSFilterXLSX iXlsxFilter;
    private boolean iSynchronizing;

    /**
     * @return the shared Excel file chooser instance.
     */
    public static SSExcelFileChooser getInstance() {
        if (cInstance == null) {
            cInstance = new SSExcelFileChooser();
        }
        return cInstance;
    }

    /**
     *
     */
    private SSExcelFileChooser() {
        iXlsFilter = new SSFilterXLS();
        iXlsxFilter = new SSFilterXLSX();

        // Add a custom file filter
        addChoosableFileFilter(iXlsxFilter);
        addChoosableFileFilter(iXlsFilter);
        addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY,
                e -> synchronizeSelectedFileExtension());
        setFileFilter(iXlsxFilter);
        // Disable the default (Accept All) file filter.
        setAcceptAllFileFilterUsed(false);
    }

    @Override
    public void setSelectedFile(File pFile) {
        super.setSelectedFile(pFile);
        synchronizeFilterFromSelectedFile();
    }

    @Override
    public int showOpenDialog(Component pParent) throws HeadlessException {
        // Open dialogs should default to modern .xlsx while keeping .xls selectable.
        setFileFilter(iXlsxFilter);
        synchronizeSelectedFileExtension();
        return super.showOpenDialog(pParent);
    }

    @Override
    public int showSaveDialog(Component pParent) throws HeadlessException {
        synchronizeFilterFromSelectedFile();
        synchronizeSelectedFileExtension();
        return super.showSaveDialog(pParent);
    }

    private void synchronizeFilterFromSelectedFile() {
        if (iSynchronizing) {
            return;
        }

        String iExtension = getExcelExtension(getSelectedFile());

        if ("xls".equals(iExtension)) {
            setFileFilterIfNeeded(iXlsFilter);
        } else if ("xlsx".equals(iExtension)) {
            setFileFilterIfNeeded(iXlsxFilter);
        }
    }

    private void setFileFilterIfNeeded(FileFilter pFilter) {
        if (getFileFilter() == pFilter) {
            return;
        }

        iSynchronizing = true;
        try {
            setFileFilter(pFilter);
        } finally {
            iSynchronizing = false;
        }
    }

    private void synchronizeSelectedFileExtension() {
        if (iSynchronizing) {
            return;
        }

        File iSelectedFile = getSelectedFile();
        String iTargetExtension = getCurrentFilterExtension();
        String iVisibleFileName = getVisibleFileName();

        if (iTargetExtension == null) {
            return;
        }

        String iUpdatedVisibleFileName = replaceExcelExtension(iVisibleFileName, iTargetExtension);
        File iUpdatedFile = replaceExcelExtension(iSelectedFile, iTargetExtension);

        if (iUpdatedFile == null) {
            iUpdatedFile = createSelectedFile(iUpdatedVisibleFileName);
        }

        if (hasSameFileName(iSelectedFile, iUpdatedFile)
                && equalsNullable(iVisibleFileName, iUpdatedVisibleFileName)) {
            return;
        }

        iSynchronizing = true;
        try {
            setVisibleFileName(iUpdatedVisibleFileName);

            if (iUpdatedFile != null) {
                super.setSelectedFile(iUpdatedFile);
            }
        } finally {
            iSynchronizing = false;
        }
    }

    private String getCurrentFilterExtension() {
        FileFilter iCurrentFilter = getFileFilter();

        if (iCurrentFilter == iXlsFilter) {
            return "xls";
        }
        if (iCurrentFilter == iXlsxFilter) {
            return "xlsx";
        }
        return null;
    }

    private File replaceExcelExtension(File pFile, String pExtension) {
        if (pFile == null || pFile.isDirectory()) {
            return pFile;
        }

        String iFileName = pFile.getName();
        String iUpdatedFileName = replaceExcelExtension(iFileName, pExtension);

        if (iFileName.equals(iUpdatedFileName)) {
            return pFile;
        }

        return pFile.getParentFile() == null ? new File(iUpdatedFileName)
                : new File(pFile.getParentFile(), iUpdatedFileName);
    }

    private String replaceExcelExtension(String pFileName, String pExtension) {
        if (pFileName == null) {
            return null;
        }

        String iFileName = pFileName.trim();

        if (iFileName.isEmpty()) {
            return pFileName;
        }

        int iDotIndex = iFileName.lastIndexOf('.');
        String iCurrentExtension = iDotIndex > 0 && iDotIndex < iFileName.length() - 1
                ? iFileName.substring(iDotIndex + 1).toLowerCase(Locale.ROOT) : "";

        if (iDotIndex > 0 && !"xls".equals(iCurrentExtension) && !"xlsx".equals(iCurrentExtension)) {
            return pFileName;
        }

        String iBaseName = iDotIndex > 0 ? iFileName.substring(0, iDotIndex) : iFileName;
        return iBaseName + '.' + pExtension;
    }

    private String getVisibleFileName() {
        if (getUI() instanceof BasicFileChooserUI) {
            return ((BasicFileChooserUI) getUI()).getFileName();
        }

        File iSelectedFile = getSelectedFile();

        return iSelectedFile == null ? null : iSelectedFile.getName();
    }

    private void setVisibleFileName(String pFileName) {
        if (getUI() instanceof BasicFileChooserUI) {
            ((BasicFileChooserUI) getUI()).setFileName(pFileName);
        }
    }

    private File createSelectedFile(String pFileName) {
        if (pFileName == null) {
            return null;
        }

        String iFileName = pFileName.trim();

        if (iFileName.isEmpty()) {
            return null;
        }

        File iFile = new File(iFileName);

        if (iFile.isAbsolute()) {
            return iFile;
        }

        File iCurrentDirectory = getCurrentDirectory();

        return iCurrentDirectory == null ? iFile : new File(iCurrentDirectory, iFileName);
    }

    private boolean hasSameFileName(File pFirst, File pSecond) {
        if (pFirst == pSecond) {
            return true;
        }
        if (pFirst == null || pSecond == null) {
            return false;
        }

        return pFirst.equals(pSecond);
    }

    private boolean equalsNullable(String pFirst, String pSecond) {
        return pFirst == null ? pSecond == null : pFirst.equals(pSecond);
    }

    private String getExcelExtension(File pFile) {
        if (pFile == null) {
            return "";
        }

        String iFileName = pFile.getName();
        int iDotIndex = iFileName.lastIndexOf('.');

        if (iDotIndex < 0 || iDotIndex >= iFileName.length() - 1) {
            return "";
        }

        return iFileName.substring(iDotIndex + 1).toLowerCase(Locale.ROOT);
    }

}
