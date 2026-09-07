package se.swedsoft.bookkeeping.gui.accountplans;

import org.junit.jupiter.api.Test;
import se.swedsoft.bookkeeping.data.SSAccount;
import se.swedsoft.bookkeeping.data.SSAccountPlan;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SSAccountPlanDialogTest {

    @Test
    void hasUnsavedChangesReturnsFalseForUnchangedCopy() throws Exception {
        SSAccountPlan plan = samplePlan();
        SSAccountPlan snapshot = createSnapshot(plan);

        boolean hasUnsavedChanges = hasUnsavedChanges(plan, snapshot);

        assertThat(hasUnsavedChanges).isFalse();
    }

    @Test
    void hasUnsavedChangesReturnsTrueWhenAccountDescriptionChanges() throws Exception {
        SSAccountPlan plan = samplePlan();
        SSAccountPlan snapshot = createSnapshot(plan);
        plan.getAccounts().get(0).setDescription("Updated");

        boolean hasUnsavedChanges = hasUnsavedChanges(plan, snapshot);

        assertThat(hasUnsavedChanges).isTrue();
    }

    @Test
    void hasUnsavedChangesReturnsTrueWhenBaseNameChanges() throws Exception {
        SSAccountPlan plan = samplePlan();
        SSAccountPlan snapshot = createSnapshot(plan);
        plan.setBaseName("Updated base");

        boolean hasUnsavedChanges = hasUnsavedChanges(plan, snapshot);

        assertThat(hasUnsavedChanges).isTrue();
    }

    private static SSAccountPlan createSnapshot(SSAccountPlan plan) throws Exception {
        Method createSnapshot = SSAccountPlanDialog.class.getDeclaredMethod("createAccountPlanSnapshot", SSAccountPlan.class);
        createSnapshot.setAccessible(true);
        return (SSAccountPlan) createSnapshot.invoke(null, plan);
    }

    private static boolean hasUnsavedChanges(SSAccountPlan plan, SSAccountPlan snapshot) throws Exception {
        Method hasUnsavedChanges = SSAccountPlanDialog.class.getDeclaredMethod("hasUnsavedChanges", SSAccountPlan.class,
                SSAccountPlan.class);
        hasUnsavedChanges.setAccessible(true);
        return (boolean) hasUnsavedChanges.invoke(null, plan, snapshot);
    }

    private static SSAccountPlan samplePlan() {
        SSAccountPlan plan = new SSAccountPlan();
        plan.setName("Plan");
        plan.setBaseName("Base");
        plan.setAssessementYear("2026");

        SSAccount account = new SSAccount(1910);
        account.setDescription("Cash");
        account.setVATCode("A");
        account.setSRUCode("1000");
        account.setReportCode("R1");
        account.setActive(true);
        account.setProjectRequired(false);
        account.setResultUnitRequired(false);
        plan.addAccount(account);

        return plan;
    }
}
