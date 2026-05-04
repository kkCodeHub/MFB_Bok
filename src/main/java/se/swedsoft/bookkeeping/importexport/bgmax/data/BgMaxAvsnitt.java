package se.swedsoft.bookkeeping.importexport.bgmax.data;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Represents a section (avsnitt/insättning) in a BgMax file.
 *
 * User: Andreas Lago
 * Date: 2006-aug-23
 * Time: 11:21:49
 */
public class BgMaxAvsnitt {

    private String iBankgiroNummer;
    private String iPlusgiroNummer;
    private String iBankKontoNummer;
    private String iBetalningsdag;
    private String iLopnummer;
    private String iBelopp;
    private String iValuta;
    private String iAntal;
    private String iTyp;

    private final List<BgMaxBetalning> iBetalningar;

    /**
     *
     */
    public BgMaxAvsnitt() {
        iBetalningar = new LinkedList<>();
    }

    // ---- Getters and Setters ----

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
     * @return the plusgiro number
     */
    public String getPlusgiroNummer() {
        return iPlusgiroNummer;
    }

    /**
     * @param iPlusgiroNummer the plusgiro number
     */
    public void setPlusgiroNummer(String iPlusgiroNummer) {
        this.iPlusgiroNummer = iPlusgiroNummer;
    }

    /**
     * @return the bank account number
     */
    public String getBankKontoNummer() {
        return iBankKontoNummer;
    }

    /**
     * @param iBankKontoNummer the bank account number
     */
    public void setBankKontoNummer(String iBankKontoNummer) {
        this.iBankKontoNummer = iBankKontoNummer;
    }

    /**
     * @return the payment date string
     */
    public String getBetalningsdag() {
        return iBetalningsdag;
    }

    /**
     * @param iBetalningsdag the payment date string
     */
    public void setBetalningsdag(String iBetalningsdag) {
        this.iBetalningsdag = iBetalningsdag;
    }

    /**
     * @return the serial number
     */
    public String getLopnummer() {
        return iLopnummer;
    }

    /**
     * @param iLopnummer the serial number
     */
    public void setLopnummer(String iLopnummer) {
        this.iLopnummer = iLopnummer;
    }

    /**
     * @return the amount as a raw string
     */
    public String getBelopp() {
        return iBelopp;
    }

    /**
     * @param iBelopp the amount as a raw string
     */
    public void setBelopp(String iBelopp) {
        this.iBelopp = iBelopp;
    }

    /**
     * @return the currency code
     */
    public String getValuta() {
        return iValuta;
    }

    /**
     * @param iValuta the currency code
     */
    public void setValuta(String iValuta) {
        this.iValuta = iValuta;
    }

    /**
     * @return the count
     */
    public String getAntal() {
        return iAntal;
    }

    /**
     * @param iAntal the count
     */
    public void setAntal(String iAntal) {
        this.iAntal = iAntal;
    }

    /**
     * @return the type
     */
    public String getTyp() {
        return iTyp;
    }

    /**
     * @param iTyp the type
     */
    public void setTyp(String iTyp) {
        this.iTyp = iTyp;
    }

    /**
     * @return an unmodifiable view of the payments in this section
     */
    public List<BgMaxBetalning> getBetalningar() {
        return Collections.unmodifiableList(iBetalningar);
    }

    /**
     * Adds a payment to this section.
     *
     * @param iBetalning the payment to add
     */
    public void addBetalning(BgMaxBetalning iBetalning) {
        iBetalningar.add(iBetalning);
    }

    /**
     * @return the last payment in this section, or {@code null} if empty
     */
    public BgMaxBetalning getLastBetalning() {
        return iBetalningar.isEmpty() ? null : iBetalningar.get(iBetalningar.size() - 1);
    }

    /**
     * @return {@code true} if this section has no payments
     */
    public boolean hasNoBetalningar() {
        return iBetalningar.isEmpty();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("  Avsnitt:\n");
        sb.append("  {\n");
        sb.append("    iBankgiroNummer  : ").append(iBankgiroNummer).append('\n');
        sb.append("    iPlusgiroNummer  : ").append(iPlusgiroNummer).append('\n');
        sb.append("    iValuta          : ").append(iValuta).append('\n');

        for (BgMaxBetalning iBetalning : iBetalningar) {
            sb.append(iBetalning);
        }

        sb.append("    iBankKontoNummer : ").append(iBankKontoNummer).append('\n');
        sb.append("    iBetalningsdag   : ").append(iBetalningsdag).append('\n');
        sb.append("    iLopnummer       : ").append(iLopnummer).append('\n');
        sb.append("    iBelopp          : ").append(iBelopp).append('\n');
        sb.append("    iValuta          : ").append(iValuta).append('\n');
        sb.append("    iAntal           : ").append(iAntal).append('\n');
        sb.append("    iTyp             : ").append(iTyp).append('\n');
        sb.append("  }\n");

        return sb.toString();
    }

}
