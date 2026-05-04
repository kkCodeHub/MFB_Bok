package se.swedsoft.bookkeeping.importexport.bgmax.data;


/**
 * Represents an extra reference number record in a BgMax file.
 *
 * User: Andreas Lago
 * Date: 2006-aug-24
 * Time: 10:28:30
 */
public class BgMaxReferens {

    private String iBankgiroNummer;
    private String iReferens;
    private String iBelopp;
    private String iReferensKod;
    private String iBetalningsKanalKod;
    private String iBGCLopnummer;
    private String iAvibildmarkering;

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

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("    Referens:\n");
        sb.append("    {\n");
        sb.append("        iBankgiroNummer     : ").append(iBankgiroNummer).append('\n');
        sb.append("        iReferens           : ").append(iReferens).append('\n');
        sb.append("        iBelopp             : ").append(iBelopp).append('\n');
        sb.append("        iReferensKod        : ").append(iReferensKod).append('\n');
        sb.append("        iBetalningsKanalKod : ").append(iBetalningsKanalKod).append(
                '\n');
        sb.append("        iBGCLopnummer       : ").append(iBGCLopnummer).append('\n');
        sb.append("        iBankgiroNummer     : ").append(iBankgiroNummer).append('\n');
        sb.append("        iAvibildmarkering   : ").append(iAvibildmarkering).append('\n');
        sb.append("    }\n");

        return sb.toString();
    }
}
