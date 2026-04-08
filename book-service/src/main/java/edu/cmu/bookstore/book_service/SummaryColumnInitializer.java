package edu.cmu.bookstore.book_service;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SummaryColumnInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SummaryColumnInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();
            String bookTableName = findTableName(metaData, "Book");

            if (bookTableName == null) {
                return;
            }

            if (productName.contains("mysql") || productName.contains("mariadb")) {
                jdbcTemplate.execute("ALTER TABLE " + bookTableName + " MODIFY COLUMN summary LONGTEXT");
            } else if (productName.contains("h2")) {
                jdbcTemplate.execute("ALTER TABLE " + bookTableName + " ALTER COLUMN summary CLOB");
            }
        } catch (Exception ex) {
            System.err.println("Summary column check skipped: " + ex.getMessage());
        }
    }

    private String findTableName(DatabaseMetaData metaData, String expectedName) throws Exception {
        try (ResultSet tables = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (tableName != null && tableName.equalsIgnoreCase(expectedName)) {
                    return tableName;
                }
            }
        }

        return null;
    }
}
