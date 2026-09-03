package se.swedsoft.bookkeeping.importexport.sie;


import org.fribok.bookkeeping.app.SSDBUiInitializer;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.data.SSNewCompany;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewProject;
import se.swedsoft.bookkeeping.data.SSNewResultUnit;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.system.SSCompanyYearContext;
import se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext;
import se.swedsoft.bookkeeping.gui.util.SSBundleString;
import se.swedsoft.bookkeeping.gui.util.frame.SSFrameManager;
import se.swedsoft.bookkeeping.importexport.sie.fields.SIEEntry;
import se.swedsoft.bookkeeping.importexport.sie.fields.SIEEntryVerifikation;
import se.swedsoft.bookkeeping.importexport.sie.types.SIEDimension;
import se.swedsoft.bookkeeping.importexport.sie.util.*;
import se.swedsoft.bookkeeping.importexport.util.SSExportException;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-feb-20
 * Time: 12:52:32
 */
public class SSSIEImporter {    private static final Logger LOG = LoggerFactory.getLogger(SSSIEImporter.class);


    private List<String> iLines;

    private SIEType iType;

    private List<SIEDimension> iDimensions;

    // Get the importer factory
    private SIEFactory iFactory;

    private File iFile;

    /**
     *
     * @param iFile
     */
    public SSSIEImporter(File iFile) {
        this.iFile = iFile;

        iLines = new LinkedList<>();
        iDimensions = SIEDimension.getDefaultDimensions();
        iFactory = SIEFactory.getImportInstance();
    }

    /**
     *
     * @throws SSImportException
     */
    public void doImport() throws SSImportException {
        SSEventTriggerSyncContext.dropTriggers();
        try {
            SSNewAccountingYear iAccountingYear = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear();
            SSNewCompany iCurrentCompany = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentCompany();

            // Read the contents of the file
            readFile(iFile);

            if (iLines.isEmpty()) {
                return;
            }

            SIEIdentityData iIdentityData = validateImportIdentity(iAccountingYear, iCurrentCompany);

            confirmAccountPlanReplacementIfNeeded(iAccountingYear);

            prepareSuggestedImportedAccountPlan(iAccountingYear, iCurrentCompany, iIdentityData);

            // Clear all active data

            iAccountingYear.getInBalance().clear();
            iAccountingYear.getBudget().clear();
            iAccountingYear.getAccountPlan().clear();
            // se.swedsoft.bookkeeping.data.system.SSAccountingContext.updateAccountingYear(iAccountingYear);

            List<SSVoucher> vouchersToDelete = new LinkedList<>(se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVouchers());
            for (SSVoucher iVoucher : vouchersToDelete) {
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.deleteVoucher(iVoucher);
            }
            se.swedsoft.bookkeeping.data.system.SSAccountingContext.getVouchers().clear();

            List<SSNewProject> projectsToDelete = new LinkedList<>(
                    se.swedsoft.bookkeeping.data.system.SSProjectContext.getProjects());

            for (SSNewProject iProject : projectsToDelete) {
                se.swedsoft.bookkeeping.data.system.SSProjectContext.deleteProject(iProject);
            }
            projectsToDelete = null;
            List<SSNewResultUnit> resultUnitsToDelete = new LinkedList<>(
                    se.swedsoft.bookkeeping.data.system.SSResultUnitContext.getResultUnits());

            for (SSNewResultUnit iResultUnit : resultUnitsToDelete) {
                se.swedsoft.bookkeeping.data.system.SSResultUnitContext.deleteResultUnit(iResultUnit);
            }
            resultUnitsToDelete = null;
            // LOG.info(iFactory.toString());

            List<List<String>> iParsedLines = getParsedLines(iLines);

            // iAccountingYear = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear();
            for (List<String> iEntryLines : iParsedLines) {
                SIEReader iReader = new SIEReader(iEntryLines);

                String iLabel = iReader.next();

                SIEEntry iEntry = iFactory.get(iLabel);

                if (iEntry != null) {
                    iEntry.importEntry(this, iReader, iAccountingYear);
                } else {
                    LOG.info("(SSSIEImporter)Missing reader for: " + iLabel);
                }
            }
            // In schema V2, SIE-imported account plans are year/company-owned only.
            se.swedsoft.bookkeeping.data.system.SSAccountingContext.updateAccountingYear(iAccountingYear);
            SSCompanyYearContext.notifyListeners("YEAR", SSCompanyYearContext.getCurrentYear(),
                    null);

            SSFrameManager.getInstance().close();
            SSDBUiInitializer.initYear(true);
        } finally {
            SSEventTriggerSyncContext.createTriggers();
        }
    }

    private SIEIdentityData validateImportIdentity(SSNewAccountingYear pCurrentYear, SSNewCompany pCurrentCompany)
            throws SSImportException {
        SIEIdentityData iData = extractIdentityData(iLines);

        if (hasCompanyIdentityWarning(iData, pCurrentCompany)) {
            boolean iContinueImport = showWarningWithAbort(
                    "SIE varning: foretagsuppgifter",
                    buildCompanyIdentityWarning(iData, pCurrentCompany)
            );
            if (!iContinueImport) {
                throw new SSImportException("Import avbruten av anvandaren efter kontroll av ORGNR/FNAMN.");
            }
        }

        if (isRar0Mismatch(iData, pCurrentYear)) {
            showWarning(
                    "SIE varning: RAR 0 avviker",
                    buildRarMismatchWarning(iData, pCurrentYear)
            );
            throw new SSImportException("Import avbruten: RAR 0 i SIE-filen avviker fran oppet bokforingsar.");
        }

        return iData;
    }

    private static void prepareSuggestedImportedAccountPlan(
            SSNewAccountingYear pCurrentYear,
            SSNewCompany pCurrentCompany,
            SIEIdentityData pIdentityData) {
        if (pCurrentYear == null) {
            return;
        }

        SSAccountPlan iImportedPlan = new SSAccountPlan(pCurrentYear.getAccountPlan());
        iImportedPlan.setId(null);
        iImportedPlan.clear();
        String iSuggestedName = buildSuggestedImportedAccountPlanName(pCurrentYear, pCurrentCompany, pIdentityData);
        iImportedPlan.setName(iSuggestedName);
        // Default to the same value for now; future SIE parsing can provide a dedicated base name.
        iImportedPlan.setBaseName(iSuggestedName);
        pCurrentYear.setAccountPlan(iImportedPlan);
    }

    private void confirmAccountPlanReplacementIfNeeded(SSNewAccountingYear pCurrentYear) throws SSImportException {
        if (pCurrentYear == null || pCurrentYear.getId() == null) {
            return;
        }

        final boolean iHasExistingRows =
                se.swedsoft.bookkeeping.data.system.SSAccountingContext.hasAccountRowsForYear(pCurrentYear.getId());

        if (!iHasExistingRows) {
            return;
        }

        boolean iContinueImport = showWarningWithAbort(
                "SIE varning: ersatt kontoplan",
                "Det finns redan en kontoplan i aktivt bokforingsar.\n\n"
                        + "Om du fortsatter kommer den befintliga kontoplanen att ersattas "
                        + "med kontoplanen fran SIE-filen.\n\n"
                        + "Vill du fortsatta importen?");

        if (!iContinueImport) {
            throw new SSImportException("Import avbruten av anvandaren innan ersattning av kontoplan.");
        }
    }

    private static String buildSuggestedImportedAccountPlanName(
            SSNewAccountingYear pCurrentYear,
            SSNewCompany pCurrentCompany,
            SIEIdentityData pIdentityData) {
        String iCompanyName = resolveSuggestedCompanyName(pCurrentCompany, pIdentityData);
        Integer iYear = resolveSuggestedYear(pCurrentYear, pIdentityData);

        return "SIEimp " + iCompanyName + " " + iYear;
    }

    private static String resolveSuggestedCompanyName(SSNewCompany pCurrentCompany, SIEIdentityData pIdentityData) {
        String iSieName = trimToNull(pIdentityData == null ? null : pIdentityData.iSieFnamn);
        if (iSieName != null) {
            return iSieName;
        }

        String iCurrentName = trimToNull(pCurrentCompany == null ? null : pCurrentCompany.getName());
        if (iCurrentName != null) {
            return iCurrentName;
        }

        return "Okant foretag";
    }

    private static Integer resolveSuggestedYear(SSNewAccountingYear pCurrentYear, SIEIdentityData pIdentityData) {
        if (pCurrentYear != null && pCurrentYear.getLocalFrom() != null) {
            return pCurrentYear.getLocalFrom().getYear();
        }

        if (pIdentityData != null && pIdentityData.iRar0From != null) {
            return pIdentityData.iRar0From.getYear();
        }

        return LocalDate.now().getYear();
    }

    private static String trimToNull(String pValue) {
        if (pValue == null) {
            return null;
        }

        String iTrimmed = pValue.trim().replaceAll("\\s+", " ");
        return iTrimmed.isEmpty() ? null : iTrimmed;
    }

    private static SIEIdentityData extractIdentityData(List<String> pLines) {
        SIEIdentityData iData = new SIEIdentityData();
        List<List<String>> iParsedLines = getParsedLines(pLines);

        for (List<String> iEntryLines : iParsedLines) {
            SIEReader iReader = new SIEReader(iEntryLines);
            if (!iReader.hasNext()) {
                continue;
            }

            String iLabel = iReader.next();

            if (SIELabel.SIE_ORGNR.getName().equals(iLabel)) {
                if (iReader.hasNextString()) {
                    iData.iSieOrgnr = iReader.nextString();
                }
                continue;
            }

            if (SIELabel.SIE_FNAMN.getName().equals(iLabel)) {
                if (iReader.hasNextString()) {
                    iData.iSieFnamn = iReader.nextString();
                }
                continue;
            }

            if (SIELabel.SIE_RAR.getName().equals(iLabel) && iReader.hasNextInteger()) {
                int iRar = iReader.nextInteger().orElse(Integer.MIN_VALUE);
                if (iRar == 0 && iReader.hasNextDate()) {
                    iData.iRar0From = SSDateUtil.toLocalDate(iReader.nextDate());
                    if (iReader.hasNextDate()) {
                        iData.iRar0To = SSDateUtil.toLocalDate(iReader.nextDate());
                    }
                }
            }
        }

        return iData;
    }

    private static boolean hasCompanyIdentityWarning(SIEIdentityData pData, SSNewCompany pCurrentCompany) {
        String iCurrentOrgnr = normalizeOrgnr(pCurrentCompany == null ? null : pCurrentCompany.getCorporateID());
        String iSieOrgnr = normalizeOrgnr(pData.iSieOrgnr);
        String iCurrentName = normalizeName(pCurrentCompany == null ? null : pCurrentCompany.getName());
        String iSieName = normalizeName(pData.iSieFnamn);

        boolean iOrgnrWarning = iSieOrgnr == null || iCurrentOrgnr == null || !iSieOrgnr.equals(iCurrentOrgnr);
        boolean iNameWarning = iSieName == null || iCurrentName == null || !iSieName.equals(iCurrentName);

        return iOrgnrWarning || iNameWarning;
    }

    private static boolean isRar0Mismatch(SIEIdentityData pData, SSNewAccountingYear pCurrentYear) {
        if (pData.iRar0From == null || pData.iRar0To == null || pCurrentYear == null) {
            return false;
        }

        LocalDate iCurrentFrom = pCurrentYear.getLocalFrom();
        LocalDate iCurrentTo = pCurrentYear.getLocalTo();

        if (iCurrentFrom == null || iCurrentTo == null) {
            return false;
        }

        return !pData.iRar0From.equals(iCurrentFrom) || !pData.iRar0To.equals(iCurrentTo);
    }

    private static String buildCompanyIdentityWarning(SIEIdentityData pData, SSNewCompany pCurrentCompany) {
        String iSieOrgnr = displayValue(pData.iSieOrgnr);
        String iCompanyOrgnr = displayValue(pCurrentCompany == null ? null : pCurrentCompany.getCorporateID());
        String iSieName = displayValue(pData.iSieFnamn);
        String iCompanyName = displayValue(pCurrentCompany == null ? null : pCurrentCompany.getName());

        return "SIE-filen avviker mot oppet foretag.\n\n"
                + "ORGNR i SIE: " + iSieOrgnr + "\n"
                + "ORGNR oppet foretag: " + iCompanyOrgnr + "\n\n"
                + "FNAMN i SIE: " + iSieName + "\n"
                + "FNAMN oppet foretag: " + iCompanyName + "\n\n"
                + "Importen fortsatter efter denna varning.";
    }

    private static String buildRarMismatchWarning(SIEIdentityData pData, SSNewAccountingYear pCurrentYear) {
        String iSiePeriod = displayPeriod(pData.iRar0From, pData.iRar0To);
        String iCurrentPeriod = displayPeriod(
                pCurrentYear == null ? null : pCurrentYear.getLocalFrom(),
                pCurrentYear == null ? null : pCurrentYear.getLocalTo());

        return "RAR 0 i SIE-filen skiljer sig fran oppet bokforingsar.\n\n"
                + "RAR 0 i SIE: " + iSiePeriod + "\n"
                + "Oppet bokforingsar: " + iCurrentPeriod + "\n\n"
                + "Importen avbryts nar du klickar OK.";
    }

    private static String normalizeOrgnr(String pValue) {
        if (pValue == null) {
            return null;
        }
        String iNormalized = pValue.replaceAll("[^0-9]", "");
        return iNormalized.isEmpty() ? null : iNormalized;
    }

    private static String normalizeName(String pValue) {
        if (pValue == null) {
            return null;
        }
        String iNormalized = pValue.trim().replaceAll("\\s+", " ").toLowerCase();
        return iNormalized.isEmpty() ? null : iNormalized;
    }

    private static String displayValue(String pValue) {
        if (pValue == null || pValue.trim().isEmpty()) {
            return "(saknas)";
        }
        return pValue.trim();
    }

    private static String displayPeriod(LocalDate pFrom, LocalDate pTo) {
        if (pFrom == null || pTo == null) {
            return "(saknas)";
        }
        return pFrom + " - " + pTo;
    }

    private static void showWarning(String pTitle, String pMessage) {
        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("{}: {}", pTitle, pMessage);
            return;
        }

        JOptionPane.showMessageDialog(
                null,
                pMessage,
                pTitle,
                JOptionPane.WARNING_MESSAGE);
    }

    private static boolean showWarningWithAbort(String pTitle, String pMessage) {
        if (GraphicsEnvironment.isHeadless()) {
            LOG.warn("{}: {}", pTitle, pMessage);
            return true;
        }

        int iChoice = JOptionPane.showOptionDialog(
                null,
                pMessage,
                pTitle,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[]{"Fortsatt import", "Avbryt import"},
                "Fortsatt import");

        return iChoice == JOptionPane.YES_OPTION;
    }

    private static final class SIEIdentityData {
        private String iSieOrgnr;
        private String iSieFnamn;
        private LocalDate iRar0From;
        private LocalDate iRar0To;
    }

    /**
     *
     * @throws SSImportException
     */
    public void doImportVouchers() throws SSImportException {
        // Read the contents of the file
        readFile(iFile);

        if (iLines.isEmpty()) {
            return;
        }

        List<List<String>> iParsedLines = getParsedLines(iLines);
        SSNewAccountingYear iYear = se.swedsoft.bookkeeping.data.system.SSCompanyYearContext.getCurrentYear();
        validateVoucherNumbersBeforeImport(iParsedLines);

        for (List<String> iEntryLines : iParsedLines) {
            SIEReader iReader = new SIEReader(iEntryLines);

            String iLabel = iReader.next();

            // Only import verifications
            if (iLabel.equals("#VER")) {
                SIEEntry iEntry = iFactory.get("#VER");

                iEntry.importEntry(this, iReader, iYear);
            }
        }
    }

    private void validateVoucherNumbersBeforeImport(List<List<String>> pParsedLines) throws SSImportException {
        Set<Integer> iImportedNumbers = new HashSet<>();

        for (List<String> iEntryLines : pParsedLines) {
            SIEReader iReader = new SIEReader(iEntryLines);
            if (!iReader.hasNext()) {
                continue;
            }

            String iLabel = iReader.next();
            if (!iLabel.equals("#VER")) {
                continue;
            }

            if (!iReader.hasFields(SIEReader.SIEDataType.STRING, SIEReader.SIEDataType.STRING, SIEReader.SIEDataType.STRING)) {
                throw new SSImportException(
                        SSBundleString.getString("sieimport.fielderror", iReader.peekLine()));
            }

            String iSerie = iReader.nextString();
            Integer iNumber = iReader.nextInteger().orElse(null);
            Integer iInternalNumber = SIEEntryVerifikation.toInternalVoucherNumber(iSerie, iNumber);

            if (iInternalNumber == null) {
                throw new SSImportException(
                        SSBundleString.getString("sieimport.fielderror", iReader.peekLine()));
            }

            if (!iImportedNumbers.add(iInternalNumber)) {
                throw new SSImportException(
                        "Dubblett av verifikationsnummer i SIE-filen: " + iInternalNumber
                                + ". Importen avbryts och inga verifikationer lases in.");
            }

            if (se.swedsoft.bookkeeping.data.system.SSAccountingContext.hasVoucher(iInternalNumber)) {
                throw new SSImportException(
                        "Verifikationsnummer finns redan i oppet bokforingsar: " + iInternalNumber
                                + ". Importen avbryts och inga verifikationer lases in.");
            }
        }
    }

    /**
     *
     * @param pFile
     */
    private void setReaded(File pFile) {
        // Set the flag to 1
        iLines.set(0, SIELabel.SIE_FLAGGA + " 1");

        writeFile(pFile);
    }

    /**
     *
     * @param iLines
     * @return
     */
    private static List<List<String>> getParsedLines(List<String> iLines) {
        List<List<String>> iParsedLines = new LinkedList<>();

        for (int iIndex = 0; iIndex < iLines.size(); iIndex++) {
            String iLine = iLines.get(iIndex);
            String iNext = (iIndex + 1 < iLines.size()) ? iLines.get(iIndex + 1) : "";

            // Skip any blank lines
            if (iLine == null || iLine.trim().length() == 0) {
                continue;
            }

            List<String> iEntryLines = new LinkedList<>();

            iEntryLines.add(iLine);

            if (iNext.equals("{")) {
                int iStart = iIndex + 2;

                for (iIndex = iStart; iIndex < iLines.size(); iIndex++) {
                    iLine = iLines.get(iIndex);

                    if (iLine.equals("}")) {
                        break;
                    }

                    iEntryLines.add(iLine);
                }
            }
            iParsedLines.add(iEntryLines);
        }
        return iParsedLines;
    }

    /**
     *
     * @param pFile
     * @throws SSImportException
     */
    protected void readFile(File pFile) throws SSImportException {
        try {
            iLines = SIEFile.readFile(pFile);
        } catch (FileNotFoundException ex) {
            LOG.error("Unexpected error", ex);
            throw new SSImportException(ex.getMessage());
        } catch (IOException ex) {
            LOG.error("Unexpected error", ex);
            throw new SSImportException(ex.getMessage());
        }
    }

    /**
     *
     * @param pFile
     * @throws SSImportException
     * @throws SSExportException
     */
    private void writeFile(File pFile) throws SSExportException {
        try {
            SIEFile.writeFile(pFile, iLines);
        } catch (FileNotFoundException ex) {
            LOG.error("Unexpected error", ex);
            throw new SSExportException(ex.getMessage());
        } catch (IOException ex) {
            LOG.error("Unexpected error", ex);
            throw new SSExportException(ex.getMessage());
        }
    }

    /**
     *
     * @param pType
     */
    public void setType(SIEType pType) {
        iType = pType;

    }

    /**
     *
     * @return
     */
    public SIEType getType() {
        return iType;
    }

    /**
     *
     * @return
     */
    public List<SIEDimension> getDimensions() {
        return iDimensions;
    }

    /**
     *
     * @param pNumber
     * @return
     */
    public SIEDimension getDimension(int pNumber) {
        return SIEDimension.getDimension(iDimensions, pNumber);
    }

    /**
     *
     * @param pDimensions
     */
    public void setDimensions(List<SIEDimension> pDimensions) {
        iDimensions = pDimensions;
    }

    /**
     *
     * @return
     */
    public SIEFactory getFactory() {
        return iFactory;
    }

    /**
     *
     * @param iFactory
     */
    public void setFactory(SIEFactory iFactory) {
        this.iFactory = iFactory;
    }

    /**
     *
     * @return
     */
    public List<String> getLines() {
        return iLines;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("se.swedsoft.bookkeeping.importexport.sie.SSSIEImporter");
        sb.append("{iDimensions=").append(iDimensions);
        sb.append(", iFactory=").append(iFactory);
        sb.append(", iFile=").append(iFile);
        sb.append(", iLines=").append(iLines);
        sb.append(", iType=").append(iType);
        sb.append('}');
        return sb.toString();
    }
}
