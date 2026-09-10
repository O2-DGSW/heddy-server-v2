package com.heddy.adapter.out.persistence;

import com.heddy.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaAlignmentIntegrationTest extends PostgresIntegrationTest {

    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void apiV2TablesAndColumnsExist() {
        assertThat(tableNames()).contains("hairstyle_colors", "ar_captures");
        assertThat(columnNames("treatment_records")).contains(
                "timezone", "cut_length", "cut_shape", "perm_type", "color_name", "products");
        assertThat(columnNames("analysis_jobs")).contains("analysis_id", "previous_record_id");
        assertThat(columnNames("analysis_results")).contains(
                "status", "summary_comment", "evidence_json");
        assertThat(columnNames("recommendation_reference_records"))
                .contains("reference_reason")
                .doesNotContain("reference_reason_code");
        assertThat(dataType("treatment_records", "duration_minutes")).isEqualTo("integer");
        assertThat(dataType("analysis_jobs", "attempt_count")).isEqualTo("integer");
        assertThat(dataType("analysis_jobs", "failure_message")).isEqualTo("text");
    }

    private List<String> tableNames() {
        return jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                """, String.class);
    }

    private List<String> columnNames(String tableName) {
        return jdbcTemplate.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                """, String.class, tableName);
    }

    private String dataType(String tableName, String columnName) {
        return jdbcTemplate.queryForObject("""
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, String.class, tableName, columnName);
    }
}
