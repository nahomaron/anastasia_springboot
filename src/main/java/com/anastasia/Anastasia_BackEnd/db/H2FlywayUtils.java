package com.anastasia.Anastasia_BackEnd.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class H2FlywayUtils {

    private H2FlywayUtils() {
        // Utility class
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=? AND COLUMN_NAME=?")) {
            check.setString(1, table.toUpperCase());
            check.setString(2, column.toUpperCase());
            try (ResultSet rs = check.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static boolean indexExists(Connection connection, String indexName) throws SQLException {
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME=?")) {
            check.setString(1, indexName.toUpperCase());
            try (ResultSet rs = check.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void execute(Connection connection, String statement) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(statement);
        }
    }

    private static void ensureColumn(Connection connection, String table, String column, String definition) throws SQLException {
        if (!columnExists(connection, table, column)) {
            execute(connection, definition);
        }
    }

    private static void ensureIndex(Connection connection, String indexName, String definition) throws SQLException {
        if (!indexExists(connection, indexName)) {
            execute(connection, definition);
        }
    }

    public static void ensureMemberColumns(Connection connection) throws SQLException {
        ensureColumn(connection, "members", "approved_at", "ALTER TABLE members ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "members", "inactive_at", "ALTER TABLE members ADD COLUMN inactive_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "members", "status_changed_at", "ALTER TABLE members ADD COLUMN status_changed_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "members", "status_reason", "ALTER TABLE members ADD COLUMN status_reason VARCHAR(512)");
        ensureColumn(connection, "members", "consent_version", "ALTER TABLE members ADD COLUMN consent_version VARCHAR(64)");
        ensureColumn(connection, "members", "consent_accepted_at", "ALTER TABLE members ADD COLUMN consent_accepted_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "members", "external_id", "ALTER TABLE members ADD COLUMN external_id VARCHAR(128)");
        ensureColumn(connection, "members", "source_system", "ALTER TABLE members ADD COLUMN source_system VARCHAR(64)");
        ensureColumn(connection, "members", "preferred_name", "ALTER TABLE members ADD COLUMN preferred_name VARCHAR(120)");
        ensureColumn(connection, "members", "deleted_at", "ALTER TABLE members ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE");
    }

    public static void ensureChildColumns(Connection connection) throws SQLException {
        ensureColumn(connection, "children", "approved_at", "ALTER TABLE children ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "children", "inactive_at", "ALTER TABLE children ADD COLUMN inactive_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "children", "status_changed_at", "ALTER TABLE children ADD COLUMN status_changed_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "children", "status_reason", "ALTER TABLE children ADD COLUMN status_reason VARCHAR(512)");
        ensureColumn(connection, "children", "consent_version", "ALTER TABLE children ADD COLUMN consent_version VARCHAR(64)");
        ensureColumn(connection, "children", "consent_accepted_at", "ALTER TABLE children ADD COLUMN consent_accepted_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "children", "external_id", "ALTER TABLE children ADD COLUMN external_id VARCHAR(128)");
        ensureColumn(connection, "children", "source_system", "ALTER TABLE children ADD COLUMN source_system VARCHAR(64)");
        ensureColumn(connection, "children", "preferred_name", "ALTER TABLE children ADD COLUMN preferred_name VARCHAR(120)");
        ensureColumn(connection, "children", "deleted_at", "ALTER TABLE children ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE");
    }

    public static void ensureTenantDemoTemplate(Connection connection) throws SQLException {
        ensureColumn(connection, "tenants", "is_demo_template", "ALTER TABLE tenants ADD COLUMN is_demo_template BOOLEAN NOT NULL DEFAULT FALSE");
        ensureIndex(connection, "idx_tenants_demo_template", "CREATE INDEX idx_tenants_demo_template ON tenants(is_demo_template)");
    }

    public static void ensureTenantWorkspaceLifecycle(Connection connection) throws SQLException {
        ensureColumn(connection, "tenants", "workspace_initialization_mode", "ALTER TABLE tenants ADD COLUMN workspace_initialization_mode VARCHAR(24)");
        ensureColumn(connection, "tenants", "is_demo_workspace", "ALTER TABLE tenants ADD COLUMN is_demo_workspace BOOLEAN NOT NULL DEFAULT FALSE");
        ensureColumn(connection, "tenants", "scheduled_purge_at", "ALTER TABLE tenants ADD COLUMN scheduled_purge_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "tenants", "purged_at", "ALTER TABLE tenants ADD COLUMN purged_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "tenants", "archive_scheduled_at", "ALTER TABLE tenants ADD COLUMN archive_scheduled_at TIMESTAMP WITH TIME ZONE");
        ensureColumn(connection, "tenants", "archived_at", "ALTER TABLE tenants ADD COLUMN archived_at TIMESTAMP WITH TIME ZONE");
        ensureIndex(connection, "idx_tenants_scheduled_purge_at", "CREATE INDEX idx_tenants_scheduled_purge_at ON tenants(scheduled_purge_at)");
        ensureIndex(connection, "idx_tenants_archive_scheduled_at", "CREATE INDEX idx_tenants_archive_scheduled_at ON tenants(archive_scheduled_at)");
    }
}
