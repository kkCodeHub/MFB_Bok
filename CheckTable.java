import java.sql.*;

public class CheckTable {
    public static void main(String[] args) throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        try (Connection conn = DriverManager.getConnection("jdbc:hsqldb:file:C:\Users\kalle\AppData\Local\fribok\db\JFSDB", "sa", "")) {
            conn.setReadOnly(true);
            String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_SCHEMA)='PUBLIC' AND UPPER(TABLE_NAME)='TBL_ACCOUNTPLAN'";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    System.out.println("✓ tbl_accountplan FINNS i PUBLIC-schemat");
                } else {
                    System.out.println("✗ tbl_accountplan FINNS INTE i PUBLIC-schemat");
                }
            }
        }
    }
}
