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
    void defaultConstructorProvidesAccountPlanType() {
        SSAccountPlan accountPlan = new SSAccountPlan();

        assertThat(accountPlan.getType()).isEqualTo(SSAccountPlanType.getDefault());
    }

    @Test
    void nullTypeFallsBackToDefaultAccountPlanType() {
        SSAccountPlan accountPlan = new SSAccountPlan();
        accountPlan.setType((SSAccountPlanType) null);

        assertThat(accountPlan.getType()).isEqualTo(SSAccountPlanType.getDefault());
    }

    @Test
    void bas95AndBas96AreNotSelectable() {
        assertThat(SSAccountPlanType.get("BAS95")).isNull();
        assertThat(SSAccountPlanType.get("BAS96")).isNull();
        assertThat(SSAccountPlanType.getAccountPlanTypes())
                .extracting(SSAccountPlanType::getName)
                .doesNotContain("BAS95", "BAS96");
    }

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

    @Test
    void copyConstructorKeepsIdForExistingPlan() {
        SSAccountPlan source = new SSAccountPlan();
        source.setId(42);
        source.setName("Imported plan");

        SSAccountPlan copy = new SSAccountPlan(source);

        assertThat(copy.getId()).isEqualTo(42);
        assertThat(copy.getName()).isEqualTo("Imported plan");
    }
}

