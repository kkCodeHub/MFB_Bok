package se.swedsoft.bookkeeping.importexport.supplierpayments.poster;


import se.swedsoft.bookkeeping.importexport.supplierpayments.data.SupplierPaymentConfig;
import se.swedsoft.bookkeeping.importexport.supplierpayments.util.LBinLine;
import se.swedsoft.bookkeeping.util.SSDateUtil;

import java.time.LocalDate;


/**
 *
 * Öppningspost
 */
public class LBinPostTK11 extends LBinPost {

    private String iCurrency;
    private String iBankGiroNr;
    private LocalDate   iDate;
    private LocalDate   iPaymentDate;
    private String iText;

    /**
     *
     */
    public LBinPostTK11() {}

    /**
     *
     * @param iCurrency
     */
    public LBinPostTK11(String iCurrency) {
        iBankGiroNr = SupplierPaymentConfig.getOurBankGiroAccount().replaceAll("-", "");
        iDate = SSDateUtil.today();
        this.iCurrency = iCurrency;
        iText = "LEVERANTÖRSBETALNINGAR";
    }

    /**
     *
     * @param iLine
     */
    @Override
    public void write(LBinLine iLine) {
        iLine.append("11");
        iLine.append(iBankGiroNr, 10, '0'); // 3 => 12: Bankgiro
        iLine.append(iDate, 6, "yyMMdd"); // 13 => 18: Skrivdatum
        iLine.append(iText, 22); // 19 => 40: Text
        iLine.append("", 6); // 41 => 46: betalningsdatum
        iLine.append("", 13); // 47 => 59: Blanka
        iLine.append(iCurrency, 3); // 60 => 62: Valuta fixme! - endast SEK från 20161031
        iLine.append("", 18); // 63 => 80: Blanka
    }

    /**
     *
     * @param iLine
     */
    @Override
    public void read(LBinLine iLine) {
        iBankGiroNr = iLine.readString(3, 12); // 3 => 12: Bakngiro
        iDate = iLine.readDate(13, 18, "yyMMdd"); // 13 => 18: Skrivdatum
        iText = iLine.readString(19, 40); // 19 => 40: Text
        iPaymentDate = iLine.readDate(41, 46, "yyMMdd"); // 41 => 46: betalningsdatum
        iCurrency = iLine.readString(60, 62); // 60 => 62: Valuta

    }

    /**
     *
     */
    public String getiCurrency() {
        return iCurrency;
    }

    /**
     *
     * @param iCurrency
     */
    public void setCurrency(String iCurrency) {
        this.iCurrency = iCurrency;
    }

    /**
     *
     */
    public String getBankGiroNr() {
        return iBankGiroNr;
    }

    /**
     *
     * @param iBankGiroNr
     */
    public void setBankGiroNr(String iBankGiroNr) {
        this.iBankGiroNr = iBankGiroNr;
    }

    /**
     *
     */
    public LocalDate getDate() {
        return iDate;
    }

    /**
     *
     * @param iDate
     */
    public void setDate(LocalDate iDate) {
        this.iDate = iDate;
    }

    /**
     *
     */
    public LocalDate getPaymentDate() {
        return iPaymentDate;
    }

    /**
     *
     * @param iPaymentDate
     */
    public void setPaymentDate(LocalDate iPaymentDate) {
        this.iPaymentDate = iPaymentDate;
    }

    /**
     *
     */
    public String getText() {
        return iText;
    }

    /**
     *
     * @param iText
     */
    public void setText(String iText) {
        this.iText = iText;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(
                "se.swedsoft.bookkeeping.importexport.supplierpayments.poster.LBinPostTK11");
        sb.append("{iBankGiroNr='").append(iBankGiroNr).append('\'');
        sb.append(", iCurrency='").append(iCurrency).append('\'');
        sb.append(", iDate=").append(iDate);
        sb.append(", iPaymentDate=").append(iPaymentDate);
        sb.append(", iText='").append(iText).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
