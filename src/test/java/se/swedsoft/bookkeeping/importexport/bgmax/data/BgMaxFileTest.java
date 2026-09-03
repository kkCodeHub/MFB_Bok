package se.swedsoft.bookkeeping.importexport.bgmax.data;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Tests for BGMax.
 *
 * @author jensli
 */
class BgMaxFileTest {
    private final List<List<String>> iFiles = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        iFiles.addAll(BgMaxTestFixture.readAllSampleFiles());
    }

    @Test
    void shouldParseSampleFile4() {
        BgMaxFile iBgMaxFile = new BgMaxFile();

        iBgMaxFile.parse(iFiles.get(3));

        assertEquals("BGMAX", iBgMaxFile.getLayoutnamn(), "layout name after reading");
        assertEquals("01", iBgMaxFile.getVersion(), "iVersion");
        assertEquals("20040525173035010331", iBgMaxFile.getTidsstampel(), "iTidsstampel");

        assertEquals("00000009", iBgMaxFile.getAntalBetalningsPoster(),
                "iAntalBetalningsPoster");
        assertEquals("00000000", iBgMaxFile.getAntalAvdragsPoster(), "iAntalAvdragsPoster");
        assertEquals("00000013", iBgMaxFile.getAntalExtraReferensPoster(),
                "iAntalExtraReferensPoster");
        assertEquals("00000004", iBgMaxFile.getAntalInsattningsPoster(),
                "iAntalInsattningsPoster");

        BgMaxAvsnitt iAvsnitt = iBgMaxFile.getAvsnitts().get(0);

        assertEquals("0009912346", iAvsnitt.getBankgiroNummer(), "iBankgiroNummer");
        assertEquals("SEK", iAvsnitt.getValuta(), "iValuta");

        BgMaxBetalning iBetalning = iAvsnitt.getBetalningar().get(0);

        assertEquals("0003783511", iBetalning.getBankgiroNummer(), "iBankgiroNummer");
        assertEquals("", iBetalning.getReferens(), "iReferens");
        assertEquals("000000000000180000", iBetalning.getBeloppRaw(), "iBetalningsBelopp");
        assertEquals("0", iBetalning.getReferensKod(), "iReferensKod");
        assertEquals("2", iBetalning.getBetalningsKanalKod(), "iBetalningsKanalKod");
        assertEquals("000120000018", iBetalning.getBGCLopnummer(), "iBGCLopnummer");
        assertEquals("0", iBetalning.getAvibildmarkering(), "iAvibildmarkering");
        assertEquals("Betalning med extra refnr 665869 657775 665661665760",
                iBetalning.getInformationsText(), "iInformationsText");
        assertEquals("Kalles Pl", iBetalning.getBetalarensNamn().substring(0, 9),
                "iBetalarensNamn");
        assertEquals("Storgatan 2", iBetalning.getBetalarensAdress(), "iBetalarensAdress");
        assertEquals("12345", iBetalning.getBetalarensPostnummer(), "iBetalarensPostnummer");
        assertEquals("Stor", iBetalning.getBetalarensOrt().substring(0, 4), "iBetalarensOrt");
        assertEquals("005500001234", iBetalning.getBetalarensOrganisationsnr(),
                "iBetalarensOrganisationsnr");

        assertEquals("00000000000000000005841000001009823", iAvsnitt.getBankKontoNummer(),
                "iBankKontoNummer");
        assertEquals("20040525", iAvsnitt.getBetalningsdag(), "iBetalningsdag");
        assertEquals("00056", iAvsnitt.getLopnummer(), "iLopnummer");

        assertEquals("000000000000370", iAvsnitt.getBelopp(), "iBelopp");
        assertEquals("SEK", iAvsnitt.getValuta(), "iValuta");
        assertEquals("00000002", iAvsnitt.getAntal(), "iAntal");

        BgMaxReferens iReferens = iBetalning.getReferenser().get(0);

        assertEquals("0003783511", iReferens.getBankgiroNummer(), "iBankgiroNummer");
        assertEquals("665760", iReferens.getReferens(), "iReferens");
        assertEquals("000000000000000000", iReferens.getBelopp(), "iBelopp");
        assertEquals("2", iReferens.getReferensKod(), "iReferensKod");
        assertEquals("2", iReferens.getBetalningsKanalKod(), "iBetalningsKanalKod");
        assertEquals("000120000018", iReferens.getBGCLopnummer(), "iBGCLopnummer");
        assertEquals("0", iReferens.getAvibildmarkering(), "iAvibildmarkering");
    }

}
