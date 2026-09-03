package se.swedsoft.bookkeeping.testsupport.data;

import se.swedsoft.bookkeeping.data.SSCustomer;
import se.swedsoft.bookkeeping.data.SSProduct;
import se.swedsoft.bookkeeping.data.SSVoucher;
import se.swedsoft.bookkeeping.data.SSVoucherRow;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class SSTestDataFactory {

    private SSTestDataFactory() {}

    public static SSCustomer xmlRoundTripCustomer(String pNumber) {
        SSCustomer iCustomer = new SSCustomer();
        iCustomer.setNumber(pNumber);
        iCustomer.setName("XML Kund AB");
        iCustomer.setEMail("xml.kund@fribok.se");
        iCustomer.setPhone1("08-123456");
        iCustomer.getInvoiceAddress().setName("Faktura XML AB");
        iCustomer.getInvoiceAddress().setAddress1("Fakturagatan 1");
        iCustomer.getInvoiceAddress().setCity("Stockholm");
        iCustomer.getDeliveryAddress().setName("Leverans XML AB");
        iCustomer.getDeliveryAddress().setAddress1("Leveransgatan 2");
        iCustomer.getDeliveryAddress().setCity("Uppsala");
        return iCustomer;
    }

    public static SSProduct xmlRoundTripProduct(String pNumber) {
        SSProduct iProduct = new SSProduct();
        iProduct.setNumber(pNumber);
        iProduct.setDescription("XML Produkt");
        iProduct.setSellingPrice(new BigDecimal("123.45"));
        iProduct.setPurchasePrice(new BigDecimal("67.89"));
        iProduct.setWarehouseLocation("A-01");
        iProduct.setOrderpoint(7);
        iProduct.setOrdercount(14);
        return iProduct;
    }

    public static SSVoucher balancedVoucher(int pNumber, LocalDate pDate, String pDescription,
                                            int pDebitAccount, BigDecimal pDebit,
                                            int pCreditAccount, BigDecimal pCredit) {
        SSVoucher iVoucher = new SSVoucher(pNumber);
        iVoucher.setLocalDate(pDate);
        iVoucher.setDescription(pDescription);
        iVoucher.getRows().add(voucherRow(pDebitAccount, pDebit, null));
        iVoucher.getRows().add(voucherRow(pCreditAccount, null, pCredit));
        return iVoucher;
    }

    private static SSVoucherRow voucherRow(int pAccountNumber, BigDecimal pDebet, BigDecimal pCredit) {
        SSVoucherRow iRow = new SSVoucherRow();
        iRow.setAccountNr(pAccountNumber);
        iRow.setDebet(pDebet);
        iRow.setCredit(pCredit);
        return iRow;
    }
}

