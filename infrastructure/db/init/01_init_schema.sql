-- SentinelX Database Schema - V1__initial_schema.sql
-- Core tables for authentication, payments, risk, threatlab, community, dryrun, ops

-- ============================================================
-- USERS & AUTHENTICATION
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'DEVELOPER' CHECK (role IN ('ADMIN', 'ANALYST', 'DEVELOPER', 'VIEWER')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- ============================================================
-- PAYMENTS & TRANSACTIONS
-- ============================================================
CREATE TYPE transaction_status AS ENUM (
    'CREATED', 'PROCESSING', 'RISK_REVIEW', 'AUTHORIZED', 'COMPLETED',
    'BLOCKED', 'FAILED', 'CANCELLED', 'REFUNDED'
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    merchant_id VARCHAR(100) NOT NULL,
    merchant_name VARCHAR(255),
    device_id VARCHAR(255),
    device_fingerprint VARCHAR(500),
    location_country VARCHAR(2),
    location_region VARCHAR(100),
    location_city VARCHAR(100),
    ip_address INET,
    status transaction_status NOT NULL DEFAULT 'CREATED',
    risk_score INTEGER CHECK (risk_score >= 0 AND risk_score <= 100),
    risk_decision VARCHAR(20) CHECK (risk_decision IN ('ALLOW', 'REVIEW', 'BLOCK')),
    correlation_id UUID NOT NULL DEFAULT gen_random_uuid(),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX idx_transactions_correlation_id ON transactions(correlation_id);
CREATE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);

CREATE TABLE payment_state_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    from_status transaction_status,
    to_status transaction_status NOT NULL,
    reason VARCHAR(500),
    triggered_by VARCHAR(100),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_state_history_transaction_id ON payment_state_history(transaction_id);
CREATE INDEX idx_payment_state_history_created_at ON payment_state_history(created_at DESC);

-- ============================================================
-- RISK ENGINE
-- ============================================================
CREATE TABLE risk_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    condition_json JSONB NOT NULL,
    weight INTEGER NOT NULL DEFAULT 10 CHECK (weight >= 0 AND weight <= 100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 1,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_risk_rules_enabled ON risk_rules(enabled);

CREATE TABLE risk_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('ALLOW', 'REVIEW', 'BLOCK')),
    triggered_rules JSONB NOT NULL DEFAULT '[]',
    rule_scores JSONB NOT NULL DEFAULT '{}',
    ml_score DECIMAL(5,4),
    ml_model_version VARCHAR(50),
    evaluation_duration_ms INTEGER,
    explanation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_risk_evaluations_transaction_id ON risk_evaluations(transaction_id);
CREATE INDEX idx_risk_evaluations_score ON risk_evaluations(score DESC);
CREATE INDEX idx_risk_evaluations_created_at ON risk_evaluations(created_at DESC);

-- ============================================================
-- THREAT LAB
-- ============================================================
CREATE TYPE scenario_category AS ENUM (
    'FRAUD_PATTERN', 'VELOCITY_ATTACK', 'ACCOUNT_TAKEOVER', 'API_ABUSE',
    'TRAFFIC_SPIKE', 'SERVICE_FAILURE', 'DATA_EXFILTRATION', 'CUSTOM'
);

CREATE TABLE threat_scenarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category scenario_category NOT NULL,
    input_template JSONB NOT NULL,
    expected_result JSONB NOT NULL,
    tags TEXT[] DEFAULT '{}',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_threat_scenarios_category ON threat_scenarios(category);
CREATE INDEX idx_threat_scenarios_enabled ON threat_scenarios(enabled);

CREATE TABLE simulation_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario_id UUID NOT NULL REFERENCES threat_scenarios(id),
    user_id UUID NOT NULL REFERENCES users(id),
    input_json JSONB NOT NULL,
    actual_result JSONB NOT NULL,
    expected_result JSONB NOT NULL,
    detected BOOLEAN,
    detection_details JSONB,
    duration_ms INTEGER,
    logs JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_simulation_runs_scenario_id ON simulation_runs(scenario_id);
CREATE INDEX idx_simulation_runs_user_id ON simulation_runs(user_id);
CREATE INDEX idx_simulation_runs_created_at ON simulation_runs(created_at DESC);
CREATE INDEX idx_simulation_runs_detected ON simulation_runs(detected);

-- ============================================================
-- COMMUNITY
-- ============================================================
CREATE TYPE report_type AS ENUM ('FLAW_REPORT', 'CODER_BLOCK', 'FEATURE_PROPOSAL');
CREATE TYPE report_severity AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
CREATE TYPE report_status AS ENUM ('OPEN', 'INVESTIGATING', 'IN_DEVELOPMENT', 'TESTING', 'RESOLVED', 'CLOSED', 'WONT_FIX');

CREATE TABLE problem_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type report_type NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    severity report_severity,
    component VARCHAR(255),
    status report_status NOT NULL DEFAULT 'OPEN',
    reporter_id UUID NOT NULL REFERENCES users(id),
    assignee_id UUID REFERENCES users(id),
    evidence_json JSONB DEFAULT '{}',
    reproduction_steps TEXT,
    expected_behavior TEXT,
    actual_behavior TEXT,
    tags TEXT[] DEFAULT '{}',
    related_transaction_ids UUID[],
    related_simulation_ids UUID[],
    resolution_summary TEXT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_problem_reports_type ON problem_reports(type);
CREATE INDEX idx_problem_reports_status ON problem_reports(status);
CREATE INDEX idx_problem_reports_severity ON problem_reports(severity);
CREATE INDEX idx_problem_reports_component ON problem_reports(component);
CREATE INDEX idx_problem_reports_reporter_id ON problem_reports(reporter_id);
CREATE INDEX idx_problem_reports_created_at ON problem_reports(created_at DESC);
CREATE INDEX idx_problem_reports_tags ON problem_reports USING GIN(tags);

CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES problem_reports(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    parent_comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
    is_system_message BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_report_id ON comments(report_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_comment_id ON comments(parent_comment_id);

CREATE TABLE problem_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id_1 UUID NOT NULL REFERENCES problem_reports(id) ON DELETE CASCADE,
    report_id_2 UUID NOT NULL REFERENCES problem_reports(id) ON DELETE CASCADE,
    similarity_score DECIMAL(5,4) NOT NULL CHECK (similarity_score >= 0 AND similarity_score <= 1),
    detected_by VARCHAR(50) NOT NULL CHECK (detected_by IN ('TAGS', 'EMBEDDINGS', 'MANUAL', 'AI')),
    evidence JSONB DEFAULT '{}',
    confirmed BOOLEAN,
    confirmed_by UUID REFERENCES users(id),
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(report_id_1, report_id_2)
);

CREATE INDEX idx_problem_relationships_report_id_1 ON problem_relationships(report_id_1);
CREATE INDEX idx_problem_relationships_report_id_2 ON problem_relationships(report_id_2);
CREATE INDEX idx_problem_relationships_similarity ON problem_relationships(similarity_score DESC);

CREATE TABLE community_headlines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    summary TEXT NOT NULL,
    report_count INTEGER NOT NULL DEFAULT 0,
    simulation_count INTEGER NOT NULL DEFAULT 0,
    affected_component VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'DETECTED' CHECK (status IN ('DETECTED', 'INVESTIGATING', 'IN_DEVELOPMENT', 'TESTING', 'RESOLVED')),
    severity report_severity,
    evidence_report_ids UUID[] NOT NULL,
    evidence_simulation_ids UUID[] DEFAULT '{}',
    detection_method VARCHAR(50) NOT NULL CHECK (detection_method IN ('TAGS', 'EMBEDDINGS', 'HYBRID')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_community_headlines_status ON community_headlines(status);
CREATE INDEX idx_community_headlines_created_at ON community_headlines(created_at DESC);

-- ============================================================
-- DRY RUN / REPRODUCTION
-- ============================================================
CREATE TABLE dry_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID REFERENCES problem_reports(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    baseline_rule_version INTEGER NOT NULL,
    proposed_rule_json JSONB NOT NULL,
    baseline_result_json JSONB,
    proposed_result_json JSONB,
    diff_json JSONB,
    transactions_tested INTEGER,
    false_positive_delta INTEGER,
    false_negative_delta INTEGER,
    latency_delta_ms INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_dry_runs_report_id ON dry_runs(report_id);
CREATE INDEX idx_dry_runs_status ON dry_runs(status);
CREATE INDEX idx_dry_runs_created_at ON dry_runs(created_at DESC);

-- ============================================================
-- OPERATIONS & OBSERVABILITY
-- ============================================================
CREATE TYPE incident_type AS ENUM ('ERROR_SPIKE', 'LATENCY_SPIKE', 'SERVICE_FAILURE', 'DATABASE_ISSUE', 'CACHE_ISSUE', 'EXTERNAL_DEPENDENCY');
CREATE TYPE incident_severity AS ENUM ('SEV1', 'SEV2', 'SEV3', 'SEV4');
CREATE TYPE incident_status AS ENUM ('DETECTED', 'INVESTIGATING', 'RESOLVED');

CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type incident_type NOT NULL,
    severity incident_severity NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status incident_status NOT NULL DEFAULT 'DETECTED',
    evidence_json JSONB NOT NULL DEFAULT '{}',
    hypothesis TEXT,
    probable_cause TEXT,
    ai_summary TEXT,
    affected_components TEXT[],
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    assignee_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_severity ON incidents(severity);
CREATE INDEX idx_incidents_started_at ON incidents(started_at DESC);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    correlation_id UUID,
    metadata JSONB DEFAULT '{}',
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_events_actor_id ON audit_events(actor_id);
CREATE INDEX idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_events_correlation_id ON audit_events(correlation_id);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at DESC);
CREATE INDEX idx_audit_events_action ON audit_events(action);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    reference_type VARCHAR(100),
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);