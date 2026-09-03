package se.swedsoft.bookkeeping.importexport.sie.fields;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;
import se.swedsoft.bookkeeping.data.SSNewAccountingYear;
import se.swedsoft.bookkeeping.importexport.sie.SSSIEImporter;
import se.swedsoft.bookkeeping.importexport.sie.util.SIEReader;
import se.swedsoft.bookkeeping.importexport.util.SSImportException;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for {@link SIEEntrySRU}.
 */
class SIEEntrySRUTest {

    @Test
    void importEntrySetsRUCodeWhenProvided() throws SSImportException {
        SSNewAccountingYear year = new SSNewAccountingYear();
        SSAccountPlan plan = new SSAccountPlan();
        SSAccount account = new SSAccount(1990);
        plan.addAccount(account);
        year.setAccountPlan(plan);

        SIEEntrySRU entry = new SIEEntrySRU();
        SIEReader reader = new SIEReader(Collections.singletonList("#SRU 1990 7281"));
        reader.next();
        SSSIEImporter importer = new SSSIEImporter(new File("dummy.sie"));

        boolean result = entry.importEntry(importer, reader, year);

        assertThat(result).isTrue();
        assertThat(account.getSRUCode()).isEqualTo("7281");
    }

    @Test
    void importEntrySkipsRUCodeWhenMissing() throws SSImportException {
        SSNewAccountingYear year = new SSNewAccountingYear();
        SSAccountPlan plan = new SSAccountPlan();
        SSAccount account = new SSAccount(2010);
        plan.addAccount(account);
        year.setAccountPlan(plan);

        SIEEntrySRU entry = new SIEEntrySRU();
        SIEReader reader = new SIEReader(Collections.singletonList("#SRU 2010"));
        reader.next();
        SSSIEImporter importer = new SSSIEImporter(new File("dummy.sie"));

        boolean result = entry.importEntry(importer, reader, year);

        assertThat(result).isTrue();
        assertThat(account.getSRUCode()).isNull();
    }

    @Test
    void importEntryThrowsWhenAccountNotFound() {
        SSNewAccountingYear year = new SSNewAccountingYear();
        SSAccountPlan plan = new SSAccountPlan();
        year.setAccountPlan(plan);

        SIEEntrySRU entry = new SIEEntrySRU();
        SIEReader reader = new SIEReader(Collections.singletonList("#SRU 9999 7281"));
        reader.next();
        SSSIEImporter importer = new SSSIEImporter(new File("dummy.sie"));

        assertThatCode(() -> entry.importEntry(importer, reader, year))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Missing account");
    }
}
