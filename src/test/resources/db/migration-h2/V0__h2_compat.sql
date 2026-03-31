-- H2 compatibility helpers for Postgres-specific functions/types
CREATE ALIAS IF NOT EXISTS gen_random_uuid FOR "java.util.UUID.randomUUID";
CREATE DOMAIN IF NOT EXISTS jsonb AS JSON;
CREATE ALIAS IF NOT EXISTS ensure_member_columns FOR "com.anastasia.Anastasia_BackEnd.db.H2FlywayUtils.ensureMemberColumns";
CREATE ALIAS IF NOT EXISTS ensure_child_columns FOR "com.anastasia.Anastasia_BackEnd.db.H2FlywayUtils.ensureChildColumns";
CREATE ALIAS IF NOT EXISTS ensure_tenant_demo_template_columns FOR "com.anastasia.Anastasia_BackEnd.db.H2FlywayUtils.ensureTenantDemoTemplate";
CREATE ALIAS IF NOT EXISTS ensure_tenant_workspace_columns FOR "com.anastasia.Anastasia_BackEnd.db.H2FlywayUtils.ensureTenantWorkspaceLifecycle";
