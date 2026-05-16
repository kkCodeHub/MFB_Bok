-- One-time V2 demo seed script.
-- Idempotent statements: each INSERT is guarded with NOT EXISTS.

-- Company
INSERT INTO tbl_company(name)
SELECT 'Demoföretaget'
WHERE NOT EXISTS (
    SELECT 1 FROM tbl_company WHERE name = 'Demoföretaget'
);

-- Accounting year 2024 bound to Demoföretaget
INSERT INTO tbl_accountingyear(companyid, from_date, to_date, accountplan_id)
SELECT c.id, DATE '2024-01-01', DATE '2024-12-31', (SELECT MIN(id) FROM tbl_accountplan)
FROM tbl_company c
WHERE c.name = 'Demoföretaget'
  AND NOT EXISTS (
      SELECT 1
      FROM tbl_accountingyear y
      WHERE y.companyid = c.id
        AND y.from_date = DATE '2024-01-01'
        AND y.to_date = DATE '2024-12-31'
  );

-- Customer
INSERT INTO tbl_customer(number, companyid, name)
SELECT 'K-0001', c.id, 'Demokund AB'
FROM tbl_company c
WHERE c.name = 'Demoföretaget'
  AND NOT EXISTS (
      SELECT 1 FROM tbl_customer cu WHERE cu.companyid = c.id AND cu.number = 'K-0001'
  );

-- Product
INSERT INTO tbl_product(number, companyid, description, unitprice)
SELECT 'P-0001', c.id, 'Demoprodukt', 100.00
FROM tbl_company c
WHERE c.name = 'Demoföretaget'
  AND NOT EXISTS (
      SELECT 1 FROM tbl_product p WHERE p.companyid = c.id AND p.number = 'P-0001'
  );

-- Supplier
INSERT INTO tbl_supplier(number, companyid, name)
SELECT 'L-0001', c.id, 'Demoleverantor AB'
FROM tbl_company c
WHERE c.name = 'Demoföretaget'
  AND NOT EXISTS (
      SELECT 1 FROM tbl_supplier s WHERE s.companyid = c.id AND s.number = 'L-0001'
  );

-- Vouchers (5)
INSERT INTO tbl_voucher(number, yearid, vdate, description)
SELECT 240001, y.id, DATE '2024-01-15', 'Demovoucher 1'
FROM tbl_accountingyear y
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND y.from_date = DATE '2024-01-01'
  AND y.to_date = DATE '2024-12-31'
  AND NOT EXISTS (SELECT 1 FROM tbl_voucher v WHERE v.yearid = y.id AND v.number = 240001);

INSERT INTO tbl_voucher(number, yearid, vdate, description)
SELECT 240002, y.id, DATE '2024-02-15', 'Demovoucher 2'
FROM tbl_accountingyear y
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND y.from_date = DATE '2024-01-01'
  AND y.to_date = DATE '2024-12-31'
  AND NOT EXISTS (SELECT 1 FROM tbl_voucher v WHERE v.yearid = y.id AND v.number = 240002);

INSERT INTO tbl_voucher(number, yearid, vdate, description)
SELECT 240003, y.id, DATE '2024-03-15', 'Demovoucher 3'
FROM tbl_accountingyear y
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND y.from_date = DATE '2024-01-01'
  AND y.to_date = DATE '2024-12-31'
  AND NOT EXISTS (SELECT 1 FROM tbl_voucher v WHERE v.yearid = y.id AND v.number = 240003);

INSERT INTO tbl_voucher(number, yearid, vdate, description)
SELECT 240004, y.id, DATE '2024-04-15', 'Demovoucher 4'
FROM tbl_accountingyear y
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND y.from_date = DATE '2024-01-01'
  AND y.to_date = DATE '2024-12-31'
  AND NOT EXISTS (SELECT 1 FROM tbl_voucher v WHERE v.yearid = y.id AND v.number = 240004);

INSERT INTO tbl_voucher(number, yearid, vdate, description)
SELECT 240005, y.id, DATE '2024-05-15', 'Demovoucher 5'
FROM tbl_accountingyear y
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND y.from_date = DATE '2024-01-01'
  AND y.to_date = DATE '2024-12-31'
  AND NOT EXISTS (SELECT 1 FROM tbl_voucher v WHERE v.yearid = y.id AND v.number = 240005);

-- Voucher rows (1910 debit / 3010 credit)
INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 1910, 1000.00, NULL
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240001
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 1910 AND r.debet = 1000.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 3010, NULL, 1000.00
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240001
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 3010 AND r.credit = 1000.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 1910, 1500.00, NULL
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240002
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 1910 AND r.debet = 1500.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 3010, NULL, 1500.00
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240002
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 3010 AND r.credit = 1500.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 1910, 2000.00, NULL
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240003
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 1910 AND r.debet = 2000.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 3010, NULL, 2000.00
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240003
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 3010 AND r.credit = 2000.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 1910, 2500.00, NULL
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240004
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 1910 AND r.debet = 2500.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 3010, NULL, 2500.00
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240004
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 3010 AND r.credit = 2500.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 1910, 3000.00, NULL
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240005
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 1910 AND r.debet = 3000.00
  );

INSERT INTO tbl_voucher_row(voucher_id, account_nr, debet, credit)
SELECT v.id, 3010, NULL, 3000.00
FROM tbl_voucher v
JOIN tbl_accountingyear y ON y.id = v.yearid
JOIN tbl_company c ON c.id = y.companyid
WHERE c.name = 'Demoföretaget'
  AND v.number = 240005
  AND NOT EXISTS (
      SELECT 1 FROM tbl_voucher_row r
      WHERE r.voucher_id = v.id AND r.account_nr = 3010 AND r.credit = 3000.00
  );

