package se.swedsoft.bookkeeping.calc.math;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SSAccountMath}.
 * Only tests methods that do NOT depend on SSDB.
 */
class SSAccountMathTest {

    // ---- inPeriod ----

    @Test
    void inPeriodReturnsTrueWhenAccountInRange() {
        SSAccount account = new SSAccount(1500);
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        assertThat(SSAccountMath.inPeriod(account, from, to)).isTrue();
    }

    @Test
    void inPeriodReturnsTrueAtLowerBound() {
        SSAccount account = new SSAccount(1000);
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        assertThat(SSAccountMath.inPeriod(account, from, to)).isTrue();
    }

    @Test
    void inPeriodReturnsTrueAtUpperBound() {
        SSAccount account = new SSAccount(2000);
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        assertThat(SSAccountMath.inPeriod(account, from, to)).isTrue();
    }

    @Test
    void inPeriodReturnsFalseWhenBelowRange() {
        SSAccount account = new SSAccount(500);
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        assertThat(SSAccountMath.inPeriod(account, from, to)).isFalse();
    }

    @Test
    void inPeriodReturnsFalseWhenAboveRange() {
        SSAccount account = new SSAccount(2500);
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        assertThat(SSAccountMath.inPeriod(account, from, to)).isFalse();
    }

    @Test
    void inPeriodReturnsFalseWhenAccountNumberIsNull() {
        SSAccount account = new SSAccount(); // number is null (< 1)
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        assertThat(SSAccountMath.inPeriod(account, from, to)).isFalse();
    }

    // ---- getAccounts ----

    @Test
    void getAccountsFiltersToRange() {
        List<SSAccount> accounts = Arrays.asList(
                new SSAccount(1000),
                new SSAccount(1500),
                new SSAccount(2000),
                new SSAccount(2500),
                new SSAccount(3000)
        );
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        List<SSAccount> result = SSAccountMath.getAccounts(accounts, from, to);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(SSAccount::getNumber).containsExactly(1000, 1500, 2000);
    }

    @Test
    void getAccountsReturnsEmptyForNoMatches() {
        List<SSAccount> accounts = Arrays.asList(
                new SSAccount(100),
                new SSAccount(200)
        );
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        List<SSAccount> result = SSAccountMath.getAccounts(accounts, from, to);

        assertThat(result).isEmpty();
    }

    @Test
    void getAccountsReturnsEmptyForEmptyList() {
        List<SSAccount> accounts = Collections.emptyList();
        SSAccount from = new SSAccount(1000);
        SSAccount to = new SSAccount(2000);

        List<SSAccount> result = SSAccountMath.getAccounts(accounts, from, to);

        assertThat(result).isEmpty();
    }

    // ---- getFirstAccount ----

    @Test
    void getFirstAccountReturnsLowestNumber() {
        List<SSAccount> accounts = Arrays.asList(
                new SSAccount(2000),
                new SSAccount(1000),
                new SSAccount(3000)
        );

        SSAccount result = SSAccountMath.getFirstAccount(accounts);

        assertThat(result.getNumber()).isEqualTo(1000);
    }

    @Test
    void getFirstAccountReturnsNullForEmptyList() {
        List<SSAccount> accounts = Collections.emptyList();

        SSAccount result = SSAccountMath.getFirstAccount(accounts);

        assertThat(result).isNull();
    }

    // ---- getLastAccount ----

    @Test
    void getLastAccountReturnsHighestNumber() {
        List<SSAccount> accounts = Arrays.asList(
                new SSAccount(2000),
                new SSAccount(1000),
                new SSAccount(3000)
        );

        SSAccount result = SSAccountMath.getLastAccount(accounts);

        assertThat(result.getNumber()).isEqualTo(3000);
    }

    @Test
    void getLastAccountReturnsNullForEmptyList() {
        List<SSAccount> accounts = Collections.emptyList();

        SSAccount result = SSAccountMath.getLastAccount(accounts);

        assertThat(result).isNull();
    }

    // ---- getAccountsByVATCode ----

    @Test
    void getAccountsByVATCodeFiltersByCode() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("25");
        SSAccount a2 = new SSAccount(2000);
        a2.setVATCode("12");
        SSAccount a3 = new SSAccount(3000);
        a3.setVATCode("25");

        List<SSAccount> accounts = Arrays.asList(a1, a2, a3);

        List<SSAccount> result = SSAccountMath.getAccountsByVATCode(accounts, "25");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SSAccount::getNumber).containsExactly(1000, 3000);
    }

    @Test
    void getAccountsByVATCodeWithMultipleCodes() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("25");
        SSAccount a2 = new SSAccount(2000);
        a2.setVATCode("12");
        SSAccount a3 = new SSAccount(3000);
        a3.setVATCode("6");

        List<SSAccount> accounts = Arrays.asList(a1, a2, a3);

        List<SSAccount> result = SSAccountMath.getAccountsByVATCode(accounts, "25", "6");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SSAccount::getNumber).containsExactly(1000, 3000);
    }

    @Test
    void getAccountsByVATCodeReturnsEmptyForNoMatches() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("25");

        List<SSAccount> result = SSAccountMath.getAccountsByVATCode(
                Collections.singletonList(a1), "99");

        assertThat(result).isEmpty();
    }

    // ---- getNumAccountsByVatCode ----

    @Test
    void getNumAccountsByVatCodeReturnsCorrectCount() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("25");
        SSAccount a2 = new SSAccount(2000);
        a2.setVATCode("25");
        SSAccount a3 = new SSAccount(3000);
        a3.setVATCode("12");

        List<SSAccount> accounts = Arrays.asList(a1, a2, a3);

        assertThat(SSAccountMath.getNumAccountsByVatCode(accounts, "25")).isEqualTo(2);
    }

    // ---- getAccountWithVATCode ----

    @Test
    void getAccountWithVATCodeReturnsSingleMatch() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("25");
        SSAccount defaultAccount = new SSAccount(9999);

        List<SSAccount> accounts = Collections.singletonList(a1);

        Optional<SSAccount> result = SSAccountMath.getAccountWithVATCode(accounts, "25", defaultAccount);

        assertThat(result).isPresent();
        assertThat(result.get().getNumber()).isEqualTo(1000);
    }

    @Test
    void getAccountWithVATCodeReturnsDefaultWhenNoMatch() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("12");
        SSAccount defaultAccount = new SSAccount(9999);

        List<SSAccount> accounts = Collections.singletonList(a1);

        Optional<SSAccount> result = SSAccountMath.getAccountWithVATCode(accounts, "25", defaultAccount);

        assertThat(result).isPresent();
        assertThat(result.get().getNumber()).isEqualTo(9999);
    }

    @Test
    void getAccountWithVATCodeReturnsEmptyWhenMultipleMatches() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("25");
        SSAccount a2 = new SSAccount(2000);
        a2.setVATCode("25");
        SSAccount defaultAccount = new SSAccount(9999);

        List<SSAccount> accounts = Arrays.asList(a1, a2);

        Optional<SSAccount> result = SSAccountMath.getAccountWithVATCode(accounts, "25", defaultAccount);

        assertThat(result).isEmpty();
    }

    // ---- getSumByVATCodeForAccounts ----

    @Test
    void getSumByVATCodeForAccountsSumsMatchingAccounts() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("25");
        SSAccount a2 = new SSAccount(2000);
        a2.setVATCode("25");
        SSAccount a3 = new SSAccount(3000);
        a3.setVATCode("12");

        Map<SSAccount, BigDecimal> sums = new HashMap<>();
        sums.put(a1, new BigDecimal("100.00"));
        sums.put(a2, new BigDecimal("200.00"));
        sums.put(a3, new BigDecimal("300.00"));

        BigDecimal result = SSAccountMath.getSumByVATCodeForAccounts(sums, "25");

        assertThat(result).isEqualByComparingTo("300.00");
    }

    @Test
    void getSumByVATCodeForAccountsReturnsZeroForNoMatches() {
        SSAccount a1 = new SSAccount(1000);
        a1.setVATCode("25");

        Map<SSAccount, BigDecimal> sums = new HashMap<>();
        sums.put(a1, new BigDecimal("100.00"));

        BigDecimal result = SSAccountMath.getSumByVATCodeForAccounts(sums, "99");

        assertThat(result).isEqualByComparingTo("0");
    }

    // ---- getAccountsBySRUCode ----

    @Test
    void getAccountsBySRUCodeFiltersByCode() {
        SSAccount a1 = new SSAccount(1000);
        a1.setSRUCode("7210");
        SSAccount a2 = new SSAccount(2000);
        a2.setSRUCode("7310");
        SSAccount a3 = new SSAccount(3000);
        a3.setSRUCode("7210");

        List<SSAccount> accounts = Arrays.asList(a1, a2, a3);

        List<SSAccount> result = SSAccountMath.getAccountsBySRUCode(accounts, "7210");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SSAccount::getNumber).containsExactly(1000, 3000);
    }

    @Test
    void getAccountsBySRUCodeSkipsNullSRUCode() {
        SSAccount a1 = new SSAccount(1000);
        // SRU code is null by default
        SSAccount a2 = new SSAccount(2000);
        a2.setSRUCode("7210");

        List<SSAccount> accounts = Arrays.asList(a1, a2);

        List<SSAccount> result = SSAccountMath.getAccountsBySRUCode(accounts, "7210");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNumber()).isEqualTo(2000);
    }

    // ---- getNumAccountsBySRUCode ----

    @Test
    void getNumAccountsBySRUCodeReturnsCorrectCount() {
        SSAccount a1 = new SSAccount(1000);
        a1.setSRUCode("7210");
        SSAccount a2 = new SSAccount(2000);
        a2.setSRUCode("7210");

        List<SSAccount> accounts = Arrays.asList(a1, a2);

        assertThat(SSAccountMath.getNumAccountsBySRUCode(accounts, "7210")).isEqualTo(2);
    }

    // ---- getSumBySRUCodeForAccounts ----

    @Test
    void getSumBySRUCodeForAccountsSumsMatchingAccounts() {
        SSAccount a1 = new SSAccount(1000);
        a1.setSRUCode("7210");
        SSAccount a2 = new SSAccount(2000);
        a2.setSRUCode("7210");
        SSAccount a3 = new SSAccount(3000);
        a3.setSRUCode("7310");

        Map<SSAccount, BigDecimal> sums = new HashMap<>();
        sums.put(a1, new BigDecimal("100.00"));
        sums.put(a2, new BigDecimal("200.00"));
        sums.put(a3, new BigDecimal("300.00"));

        BigDecimal result = SSAccountMath.getSumBySRUCodeForAccounts(sums, "7210");

        assertThat(result).isEqualByComparingTo("300.00");
    }
}
