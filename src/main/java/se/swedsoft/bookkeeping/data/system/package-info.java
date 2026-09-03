/**
 * Transitional data-system layer during SSDB facade teardown.
 *
 * <p>Domain ownership in this package:
 * <ul>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSCompanyYearContext}: company/year lifecycle + event wiring</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSMasterdataContext}: masterdata CRUD (units, currencies, terms)</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSProjectContext}: project CRUD</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSResultUnitContext}: result unit CRUD</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSSalesContext}: sales-flow CRUD (customers, orders, invoices)</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSPurchaseContext}: purchase-flow CRUD (suppliers, purchase orders, supplier invoices)</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSAccountingContext}: accounting domain (years, vouchers, autodist, account plans)</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSProductContext}: product CRUD</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSOwnReportContext}: own report CRUD</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSSystemConfigContext}: startup/shutdown/db-init/schema lifecycle</li>
 *   <li>{@link se.swedsoft.bookkeeping.data.system.SSEventTriggerSyncContext}: trigger handling and cache/UI sync hooks</li>
 *   <li>SSDBLifecycleContext: legacy compatibility facade (deprecated)</li>
 * </ul>
 *
 * <p>Decision for physical SSDB teardown: context classes are temporary orchestration points and should
 * move towards dedicated services/repositories rather than becoming long-term wrappers around SSDB.
 */
package se.swedsoft.bookkeeping.data.system;
