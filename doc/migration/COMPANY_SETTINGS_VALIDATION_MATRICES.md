# Company Settings Validation Matrices (V2)

This document captures three validation matrices for company settings in V2.

Sources used:
- `src/main/resources/sql/create_tables_v2.sql`
- `src/main/java/se/swedsoft/bookkeeping/data/system/SSDB.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/company/pages/SSCompanyPageGeneral.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/company/pages/SSCompanyPageAddress.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/company/pages/SSCompanyPageAdditional.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/company/pages/SSCompanyPageTax.java`
- `src/main/java/se/swedsoft/bookkeeping/gui/company/pages/SSCompanyPageAutoIncrement.java`

## Matrix 1: Field Mapping (UI -> V2 storage)

| UI field/group | V2 storage | DB type |
|---|---|---|
| Company name | `tbl_company.name` | `VARCHAR(255)` |
| Phone 1 | `tbl_company.phone` | `VARCHAR(50)` |
| Phone 2 | `tbl_company.phone2` | `VARCHAR(50)` |
| Telefax | `tbl_company.telefax` | `VARCHAR(50)` |
| Residence | `tbl_company.residence` | `VARCHAR(255)` |
| Web address | `tbl_company.web_address` | `VARCHAR(255)` |
| SMTP address (currently not active in UI flow) | `tbl_company.smtp_address` | `VARCHAR(255)` |
| Email | `tbl_company.email` | `VARCHAR(255)` |
| Contact person | `tbl_company.contact_person` | `VARCHAR(255)` |
| Tax registered | `tbl_company.tax_registered` | `BOOLEAN` |
| Corporate ID | `tbl_company.corporate_id` | `VARCHAR(50)` |
| Logotype path | `tbl_company.logotype` | `VARCHAR(500)` |
| Bank | `tbl_company.bank` | `VARCHAR(255)` |
| VAT number | `tbl_company.vat_number` | `VARCHAR(50)` |
| Bank giro | `tbl_company.bank_account` | `VARCHAR(50)` |
| Plus giro | `tbl_company.plusgiro` | `VARCHAR(50)` |
| IBAN | `tbl_company.iban` | `VARCHAR(50)` |
| SWIFT/BIC | `tbl_company.swift` | `VARCHAR(20)` |
| Delay interest | `tbl_company.delay_interest` | `DECIMAL(10,4)` |
| Reminder fee | `tbl_company.reminder_fee` | `DECIMAL(10,2)` |
| Estimated delivery | `tbl_company.estimated_delivery` | `VARCHAR(100)` |
| Tax rate 1 | `tbl_company.taxrate1` | `DECIMAL(5,2)` |
| Tax rate 2 | `tbl_company.taxrate2` | `DECIMAL(5,2)` |
| Tax rate 3 | `tbl_company.taxrate3` | `DECIMAL(5,2)` |
| Weight unit | `tbl_company.weight_unit` | `VARCHAR(50)` |
| Volume unit | `tbl_company.volume_unit` | `VARCHAR(50)` |
| Currency | `tbl_company.currency_code` | `VARCHAR(10)` + FK |
| Standard unit | `tbl_company.standard_unit` | `VARCHAR(100)` + FK |
| Default payment term | `tbl_company.default_payment_term` | `VARCHAR(100)` + FK |
| Default delivery term | `tbl_company.default_delivery_term` | `VARCHAR(100)` + FK |
| Default delivery way | `tbl_company.default_delivery_way` | `VARCHAR(100)` + FK |
| Postal address fields | `tbl_company.addr_*` | `VARCHAR(20/100/255)` |
| Delivery address fields | `tbl_company.del_addr_*` | `VARCHAR(20/100/255)` |
| Standard texts | `tbl_company_standard_text` | `text_type VARCHAR(50)`, `text_value CLOB` |
| Default accounts | `tbl_company_default_account` | `account_type VARCHAR(50)`, `account_nr INTEGER` |
| Auto increment counters | `tbl_company_autoincrement` | `counter_key VARCHAR(64)`, `next_number INTEGER` |

## Matrix 2: MVP Validation (datatype + length + fail-fast)

| Field/group | MVP validation |
|---|---|
| Company name | Trim, max 255, required on `OK` |
| Phone 1/2, telefax | Trim, max 50 |
| Residence | Trim, max 255 |
| Web address | Trim, max 255 |
| Email | Trim, max 255 |
| Contact person | Trim, max 255 |
| Tax registered | Boolean only |
| Corporate ID | Trim, max 50 |
| Logotype path | Trim, max 500 |
| Bank | Trim, max 255 |
| VAT number | Trim, max 50 |
| Bank giro | Trim, max 50 |
| Plus giro | Trim, max 50 |
| IBAN | Trim, max 50 |
| SWIFT/BIC | Trim, max 20 (already validated in `SSDB`) |
| Delay interest | Numeric, scale <= 4 |
| Reminder fee | Numeric, scale <= 2 |
| Estimated delivery | Trim, max 100 |
| Tax rates 1/2/3 | Numeric, scale <= 2 |
| Weight/volume unit | Trim, max 50 |
| Currency/Unit/Terms/Way refs | Must exist in referenced table (FK-safe) |
| Address fields (`addr_*`, `del_addr_*`) | Trim + max length according to each column |
| Standard texts | `text_type` must map to valid `SSStandardText`; `text_value` may be long |
| Default accounts | `account_type` must map to `SSDefaultAccount`; `account_nr` integer |
| Auto increment counters | Integer >= 0 |

## Matrix 3: Full Validation (format + semantics + cross-field)

| Field/group | Full validation rule | Timing |
|---|---|---|
| Email | Basic email format validation (allow empty if optional) | Direct + `OK` |
| Web address | URL format check and optional normalization | `OK` |
| Corporate ID | Country-specific format/checksum (if enabled) | `OK` |
| VAT number | Country-specific VAT format/check | `OK` |
| IBAN | MOD-97 validation | `OK` |
| SWIFT/BIC | Character class `[A-Z0-9]`; optionally strict length 8/11 | Direct + `OK` |
| Delay interest | Range policy (for example 0-100), precision guard | `OK` + save |
| Reminder fee | Non-negative, optional upper bound | `OK` + save |
| Tax rates 1/2/3 | Range 0-100 with max 2 decimals | Direct + `OK` |
| Auto increment counters | Non-negative in UI, strict parse/range at save | Direct + `OK` + save |
| Default accounts | Verify account exists in current account plan | `OK` |
| Address/postcode | Country-aware postcode validation when country present | `OK` |
| Cross-field tax | If tax registered, require VAT number (policy choice) | `OK` |
| Cross-field bank | If one banking identifier is entered, require compatible pair(s) by policy | `OK` |
| Foreign key refs | Prevent saving unresolved custom values | Save |

## Notes: currently visible model/storage gaps

These are not part of the three matrices, but should be tracked before implementation:

- `iRoundingOff`, `iVatPeriod`, `iMailServer` exist in model/UI but are not mapped into `tbl_company` V2 fields in current save/load flow.
- `smtp_address` exists in schema but SMTP input is effectively inactive in the current company UI page.

