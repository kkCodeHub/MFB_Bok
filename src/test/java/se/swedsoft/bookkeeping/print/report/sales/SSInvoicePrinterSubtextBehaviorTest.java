package se.swedsoft.bookkeeping.print.report.sales;

import org.fribok.bookkeeping.app.Path;
import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSInvoice;
import se.swedsoft.bookkeeping.data.SSNewCompany;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SSInvoicePrinterSubtextBehaviorTest {
    @Test
    void standardInvoicePrinterUsesNewSubtextBehaviorButSubclassDoesNot() {
        assertThat(SSInvoicePrinter.usesStandardInvoiceSubtextBehavior(SSInvoicePrinter.class)).isTrue();
        assertThat(SSInvoicePrinter.usesStandardInvoiceSubtextBehavior(TestOcrLikePrinter.class)).isFalse();
    }


    @Test
    void resolveRowsTemplateForSubtextSizeMapsRequiredValues() {
        assertThat(SSInvoicePrinter.resolveRowsTemplateForSubtextSize(1))
                .isEqualTo("sales/invoice.rows01.jrxml");
        assertThat(SSInvoicePrinter.resolveRowsTemplateForSubtextSize(5))
                .isEqualTo("sales/invoice.rows05.jrxml");
        assertThat(SSInvoicePrinter.resolveRowsTemplateForSubtextSize(10))
                .isEqualTo("sales/invoice.rows10.jrxml");
        assertThat(SSInvoicePrinter.resolveRowsTemplateForSubtextSize(19))
                .isEqualTo("sales/invoice.rows19.jrxml");
    }

    @Test
    void resolveRowsTemplateForSubtextSizeFallsBackToDefaultForUnknownValue() {
        assertThat(SSInvoicePrinter.resolveRowsTemplateForSubtextSize(0))
                .isEqualTo("sales/invoice.rows.jrxml");
    }

    @Test
    void resolveRowsTemplateForPrinterUsesSubtextSizeForStandardInvoiceOnly() {
        assertThat(SSInvoicePrinter.resolveRowsTemplateForPrinter(SSInvoicePrinter.class, 10))
                .isEqualTo("sales/invoice.rows10.jrxml");
        assertThat(SSInvoicePrinter.resolveRowsTemplateForPrinter(TestOcrLikePrinter.class, 10))
                .isEqualTo("sales/invoice.rows.jrxml");
    }

    @Test
    void resolveSwishParametersReturnNullWhenPrintSwishIsDisabled() {
        SSNewCompany iCompany = new SSNewCompany() {
            @Override
            public Optional<Image> getSwishImage() {
                return Optional.of(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
            }
        };
        iCompany.setSwishText("Betala med Swish");

        assertThat(SSInvoicePrinter.resolveSwishTextForInvoice(iCompany, false)).isNull();
        assertThat(SSInvoicePrinter.resolveSwishImageForInvoice(iCompany, false)).isNull();
    }

    @Test
    void resolveSwishParametersReturnCompanyValuesWhenPrintSwishIsEnabled() {
        BufferedImage iImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        SSNewCompany iCompany = new SSNewCompany() {
            @Override
            public Optional<Image> getSwishImage() {
                return Optional.of(iImage);
            }
        };
        iCompany.setSwishText("Betala med Swish");

        assertThat(SSInvoicePrinter.resolveSwishTextForInvoice(iCompany, true)).isEqualTo("Betala med Swish");
        assertThat(SSInvoicePrinter.resolveSwishImageForInvoice(iCompany, true)).isSameAs(iImage);
    }

    @Test
    void resolveStandardInvoiceSubtextTextReturnsNullWhenPrintSubtextIsDisabled() {
        SSInvoice iInvoice = new SSInvoice();
        iInvoice.setPrintSubtext(false);
        iInvoice.setFileSubtext("whatever.txt");
        iInvoice.setText("Legacy fakturatext");

        assertThat(SSInvoicePrinter.resolveStandardInvoiceSubtextText(iInvoice)).isNull();
    }

    @Test
    void resolveStandardInvoiceSubtextTextReadsSelectedFileWhenEnabled() throws Exception {
        java.nio.file.Path iInvoiceTextDir = Path.get(Path.APP_DATA).toPath().resolve("invoiceText");
        Files.createDirectories(iInvoiceTextDir);

        java.nio.file.Path iFile = iInvoiceTextDir.resolve("subtext-test-" + System.nanoTime() + ".txt");
        Files.writeString(iFile, "Subtext from selected file", StandardCharsets.UTF_8);

        try {
            SSInvoice iInvoice = new SSInvoice();
            iInvoice.setPrintSubtext(true);
            iInvoice.setFileSubtext(iFile.getFileName().toString());

            assertThat(SSInvoicePrinter.resolveStandardInvoiceSubtextText(iInvoice))
                    .isEqualTo("Subtext from selected file");
        } finally {
            Files.deleteIfExists(iFile);
        }
    }

    @Test
    void resolveStandardInvoiceSubtextTextRejectsPathTraversal() {
        SSInvoice iInvoice = new SSInvoice();
        iInvoice.setPrintSubtext(true);
        iInvoice.setFileSubtext("..\\evil.txt");

        assertThatThrownBy(() -> SSInvoicePrinter.resolveStandardInvoiceSubtextText(iInvoice))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static class TestOcrLikePrinter extends SSInvoicePrinter {
    }
}
