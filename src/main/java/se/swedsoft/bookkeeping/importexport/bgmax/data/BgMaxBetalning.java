package se.swedsoft.bookkeeping.importexport.bgmax.data;


import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Represents a payment record (betalning) in a BgMax file.
 *
 * User: Andreas Lago
 * Date: 2006-aug-23
 * Time: 12:03:14
 */
public class BgMaxBetalning {
    private BgMaxAvsnitt iAvsnitt;

    private String iBankgiroNummer;
    private String iReferens;
    private String iBelopp;
    private String iReferensKod;
    private String iBetalningsKanalKod;
    private String iBGCLopnummer;
    private String iAvibildmarkering;

    private final List<BgMaxReferens> iReferenser;

    private String iInformationsText;

    private String iBetalarensNamn;
    private String iExtraNamnfalt;
    private String iBetalarensAdress;
    private String iBetalarensPostnummer;
    private String iBetalarensOrt;
    private String iBetalarensLand;
    private String iLandKod;
    private String iBetalarensOrganisationsnr;

    /**
     *
     */
    public BgMaxBetalning() {
        iReferenser = new LinkedList<>();
    }

    // ---- Getters and Setters ----

    /**
     * @return the parent section
     */
    public BgMaxAvsnitt getAvsnitt() {
        return iAvsnitt;
    }

    /**
     * @param iAvsnitt the parent section
     */
    public void setAvsnitt(BgMaxAvsnitt iAvsnitt) {
        this.iAvsnitt = iAvsnitt;
    }

    /**
     * @return the bankgiro number
     */
    public String getBankgiroNummer() {
        return iBankgiroNummer;
    }

    /**
     * @param iBankgiroNummer the bankgiro number
     */
    public void setBankgiroNummer(String iBankgiroNummer) {
        this.iBankgiroNummer = iBankgiroNummer;
    }

    /**
     * @return the reference
     */
    public String getReferens() {
        return iReferens;
    }

    /**
     * @param iReferens the reference
     */
    public void setReferens(String iReferens) {
        this.iReferens = iReferens;
    }

    /**
     * Returns the amount as a {@link BigDecimal} scaled by 10^-2 (i.e. from ore to SEK).
     *
     * @return the amount
     */
    public BigDecimal getBelopp() {
        return new BigDecimal(iBelopp).scaleByPowerOfTen(-2);
    }

    /**
     * @return the raw amount string
     */
    public String getBeloppRaw() {
        return iBelopp;
    }

    /**
     * @param iBelopp the raw amount string
     */
    public void setBeloppRaw(String iBelopp) {
        this.iBelopp = iBelopp;
    }

    /**
     * @return the reference code
     */
    public String getReferensKod() {
        return iReferensKod;
    }

    /**
     * @param iReferensKod the reference code
     */
    public void setReferensKod(String iReferensKod) {
        this.iReferensKod = iReferensKod;
    }

    /**
     * @return the payment channel code
     */
    public String getBetalningsKanalKod() {
        return iBetalningsKanalKod;
    }

    /**
     * @param iBetalningsKanalKod the payment channel code
     */
    public void setBetalningsKanalKod(String iBetalningsKanalKod) {
        this.iBetalningsKanalKod = iBetalningsKanalKod;
    }

    /**
     * @return the BGC serial number
     */
    public String getBGCLopnummer() {
        return iBGCLopnummer;
    }

    /**
     * @param iBGCLopnummer the BGC serial number
     */
    public void setBGCLopnummer(String iBGCLopnummer) {
        this.iBGCLopnummer = iBGCLopnummer;
    }

    /**
     * @return the image marking
     */
    public String getAvibildmarkering() {
        return iAvibildmarkering;
    }

    /**
     * @param iAvibildmarkering the image marking
     */
    public void setAvibildmarkering(String iAvibildmarkering) {
        this.iAvibildmarkering = iAvibildmarkering;
    }

    /**
     * @return an unmodifiable view of the extra references
     */
    public List<BgMaxReferens> getReferenser() {
        return Collections.unmodifiableList(iReferenser);
    }

    /**
     * Adds an extra reference to this payment.
     *
     * @param iReferens the reference to add
     */
    public void addReferens(BgMaxReferens iReferens) {
        iReferenser.add(iReferens);
    }

    /**
     * @return {@code true} if this payment has no extra references
     */
    public boolean hasNoReferenser() {
        return iReferenser.isEmpty();
    }

    /**
     * @return the information text
     */
    public String getInformationsText() {
        return iInformationsText;
    }

    /**
     * @param iInformationsText the information text
     */
    public void setInformationsText(String iInformationsText) {
        this.iInformationsText = iInformationsText;
    }

    /**
     * @return the payer's name
     */
    public String getBetalarensNamn() {
        return iBetalarensNamn;
    }

    /**
     * @param iBetalarensNamn the payer's name
     */
    public void setBetalarensNamn(String iBetalarensNamn) {
        this.iBetalarensNamn = iBetalarensNamn;
    }

    /**
     * @return the extra name field
     */
    public String getExtraNamnfalt() {
        return iExtraNamnfalt;
    }

    /**
     * @param iExtraNamnfalt the extra name field
     */
    public void setExtraNamnfalt(String iExtraNamnfalt) {
        this.iExtraNamnfalt = iExtraNamnfalt;
    }

    /**
     * @return the payer's address
     */
    public String getBetalarensAdress() {
        return iBetalarensAdress;
    }

    /**
     * @param iBetalarensAdress the payer's address
     */
    public void setBetalarensAdress(String iBetalarensAdress) {
        this.iBetalarensAdress = iBetalarensAdress;
    }

    /**
     * @return the payer's postal code
     */
    public String getBetalarensPostnummer() {
        return iBetalarensPostnummer;
    }

    /**
     * @param iBetalarensPostnummer the payer's postal code
     */
    public void setBetalarensPostnummer(String iBetalarensPostnummer) {
        this.iBetalarensPostnummer = iBetalarensPostnummer;
    }

    /**
     * @return the payer's city
     */
    public String getBetalarensOrt() {
        return iBetalarensOrt;
    }

    /**
     * @param iBetalarensOrt the payer's city
     */
    public void setBetalarensOrt(String iBetalarensOrt) {
        this.iBetalarensOrt = iBetalarensOrt;
    }

    /**
     * @return the payer's country
     */
    public String getBetalarensLand() {
        return iBetalarensLand;
    }

    /**
     * @param iBetalarensLand the payer's country
     */
    public void setBetalarensLand(String iBetalarensLand) {
        this.iBetalarensLand = iBetalarensLand;
    }

    /**
     * @return the country code
     */
    public String getLandKod() {
        return iLandKod;
    }

    /**
     * @param iLandKod the country code
     */
    public void setLandKod(String iLandKod) {
        this.iLandKod = iLandKod;
    }

    /**
     * @return the payer's organisation number
     */
    public String getBetalarensOrganisationsnr() {
        return iBetalarensOrganisationsnr;
    }

    /**
     * @param iBetalarensOrganisationsnr the payer's organisation number
     */
    public void setBetalarensOrganisationsnr(String iBetalarensOrganisationsnr) {
        this.iBetalarensOrganisationsnr = iBetalarensOrganisationsnr;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("    Betalning: \n");
        sb.append("    {\n");
        sb.append("      iBankgiroNummer      : ").append(iBankgiroNummer).append('\n');
        sb.append("      iReferens            : ").append(iReferens).append('\n');
        sb.append("      iBelopp              : ").append(iBelopp).append('\n');
        sb.append("      iReferensKod         : ").append(iReferensKod).append('\n');
        sb.append("      iBetalningsKanalKod  : ").append(iBetalningsKanalKod).append('\n');
        sb.append("      iBGCLopnummer        : ").append(iBGCLopnummer).append('\n');
        sb.append("      iBankgiroNummer      : ").append(iBankgiroNummer).append('\n');
        sb.append("      iAvibildmarkering    : ").append(iAvibildmarkering).append('\n');

        for (BgMaxReferens iReferens : iReferenser) {
            sb.append(iReferens);
        }
        sb.append("      iBetalarensNamn      : ").append(iBetalarensNamn).append('\n');
        sb.append("      iExtraNamnfalt       : ").append(iExtraNamnfalt).append('\n');
        sb.append("      iBetalarensAdress              : ").append(iBetalarensAdress).append(
                '\n');
        sb.append("      iBetalarensPostnummer          : ").append(iBetalarensPostnummer).append(
                '\n');
        sb.append("      iBetalarensOrt          : ").append(iBetalarensOrt).append('\n');
        sb.append("      iBetalarensLand                : ").append(iBetalarensLand).append(
                '\n');
        sb.append("      iLandKod             : ").append(iLandKod).append('\n');
        sb.append("      iBetalarensOrganisationsnr : ").append(iBetalarensOrganisationsnr).append(
                '\n');
        sb.append("    }\n");

        return sb.toString();
    }

}
