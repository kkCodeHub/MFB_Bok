package se.swedsoft.bookkeeping.data;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for {@link SSAccountPlan}.
 */
class SSAccountPlanTest {

    @Test
    void setAccountsHandlesAccountsWithoutNumber() {
        SSAccountPlan accountPlan = new SSAccountPlan();

        SSAccount numbered = new SSAccount(3000);
        SSAccount withoutNumber = new SSAccount();

        List<SSAccount> accounts = new LinkedList<>();
        accounts.add(withoutNumber);
        accounts.add(numbered);

        assertThatCode(() -> accountPlan.setAccounts(accounts)).doesNotThrowAnyException();

        assertThat(accountPlan.getAccounts()).hasSize(2);
        assertThat(accountPlan.getAccounts().get(0).getNumber()).isEqualTo(3000);
        assertThat(accountPlan.getAccounts().get(1).getNumber()).isNull();
    }

    @Test
    void setAccountsSkipsNullAccountEntries() {
        SSAccountPlan accountPlan = new SSAccountPlan();

        List<SSAccount> accounts = new LinkedList<>();
        accounts.add(null);
        accounts.add(new SSAccount(1910));

        assertThatCode(() -> accountPlan.setAccounts(accounts)).doesNotThrowAnyException();

        assertThat(accountPlan.getAccounts()).hasSize(1);
        assertThat(accountPlan.getAccounts().get(0).getNumber()).isEqualTo(1910);
    }
}

