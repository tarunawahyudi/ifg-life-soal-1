-- Initialize database schema for insurance system (using public schema)

-- Create policyholders table
CREATE TABLE IF NOT EXISTS policyholders (
    id BIGSERIAL PRIMARY KEY,
    policyholder_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create insurance policies table
CREATE TABLE IF NOT EXISTS insurance_policies (
    id BIGSERIAL PRIMARY KEY,
    policy_number VARCHAR(50) UNIQUE NOT NULL,
    policyholder_id VARCHAR(50) NOT NULL,
    policy_type VARCHAR(30) NOT NULL CHECK (policy_type IN ('LIFE', 'HEALTH', 'AUTO', 'PROPERTY', 'TRAVEL')),
    coverage_amount DECIMAL(15,2) NOT NULL CHECK (coverage_amount > 0),
    premium_amount DECIMAL(10,2) NOT NULL CHECK (premium_amount > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (policyholder_id) REFERENCES policyholders(policyholder_id)
);

-- Create claims table
CREATE TABLE IF NOT EXISTS claims (
    id BIGSERIAL PRIMARY KEY,
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    policy_number VARCHAR(50) NOT NULL,
    claim_type VARCHAR(30) NOT NULL,
    incident_date DATE NOT NULL,
    claim_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_amount DECIMAL(15,2) NOT NULL CHECK (claimed_amount > 0),
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'PAID', 'CLOSED')),
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (policy_number) REFERENCES insurance_policies(policy_number)
);

-- Create claim assessments table for storing processed claims
CREATE TABLE IF NOT EXISTS claim_assessments (
    id BIGSERIAL PRIMARY KEY,
    claim_number VARCHAR(50) NOT NULL,
    assessor_id VARCHAR(50),
    assessment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_amount DECIMAL(15,2),
    risk_score INTEGER CHECK (risk_score >= 0 AND risk_score <= 100),
    fraud_flag BOOLEAN NOT NULL DEFAULT FALSE,
    assessment_notes TEXT,
    processing_time_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (claim_number) REFERENCES claims(claim_number)
);

-- Create claim documents table
CREATE TABLE IF NOT EXISTS claim_documents (
    id BIGSERIAL PRIMARY KEY,
    claim_number VARCHAR(50) NOT NULL,
    document_type VARCHAR(30) NOT NULL CHECK (document_type IN ('MEDICAL_REPORT', 'POLICE_REPORT', 'PHOTO', 'RECEIPT', 'OTHER')),
    document_name VARCHAR(200) NOT NULL,
    file_path VARCHAR(500),
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (claim_number) REFERENCES claims(claim_number)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_policyholders_policyholder_id ON policyholders(policyholder_id);
CREATE INDEX IF NOT EXISTS idx_policyholders_email ON policyholders(email);
CREATE INDEX IF NOT EXISTS idx_insurance_policies_policy_number ON insurance_policies(policy_number);
CREATE INDEX IF NOT EXISTS idx_insurance_policies_policyholder_id ON insurance_policies(policyholder_id);
CREATE INDEX IF NOT EXISTS idx_insurance_policies_status ON insurance_policies(status);
CREATE INDEX IF NOT EXISTS idx_claims_claim_number ON claims(claim_number);
CREATE INDEX IF NOT EXISTS idx_claims_policy_number ON claims(policy_number);
CREATE INDEX IF NOT EXISTS idx_claims_status ON claims(status);
CREATE INDEX IF NOT EXISTS idx_claims_claim_date ON claims(claim_date);
CREATE INDEX IF NOT EXISTS idx_claim_assessments_claim_number ON claim_assessments(claim_number);
CREATE INDEX IF NOT EXISTS idx_claim_assessments_fraud_flag ON claim_assessments(fraud_flag);
CREATE INDEX IF NOT EXISTS idx_claim_documents_claim_number ON claim_documents(claim_number);

-- Create trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_policyholders_updated_at BEFORE UPDATE ON policyholders FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_insurance_policies_updated_at BEFORE UPDATE ON insurance_policies FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_claims_updated_at BEFORE UPDATE ON claims FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data
INSERT INTO policyholders (policyholder_id, name, email, phone, date_of_birth, address) VALUES
('PH001', 'John Doe', 'john.doe@email.com', '+1-555-0101', '1980-05-15', '123 Main St, New York, NY 10001'),
('PH002', 'Jane Smith', 'jane.smith@email.com', '+1-555-0102', '1985-08-22', '456 Oak Ave, Los Angeles, CA 90001'),
('PH003', 'Bob Johnson', 'bob.johnson@email.com', '+1-555-0103', '1975-03-10', '789 Pine Rd, Chicago, IL 60601'),
('PH004', 'Alice Brown', 'alice.brown@email.com', '+1-555-0104', '1990-12-03', '321 Elm St, Houston, TX 77001')
ON CONFLICT (policyholder_id) DO NOTHING;

INSERT INTO insurance_policies (policy_number, policyholder_id, policy_type, coverage_amount, premium_amount, start_date, end_date) VALUES
('POL001', 'PH001', 'LIFE', 500000.00, 250.00, '2023-01-01', '2024-12-31'),
('POL002', 'PH002', 'HEALTH', 100000.00, 150.00, '2023-06-01', '2024-05-31'),
('POL003', 'PH003', 'AUTO', 75000.00, 180.00, '2023-03-15', '2024-03-14'),
('POL004', 'PH004', 'PROPERTY', 300000.00, 200.00, '2023-09-01', '2024-08-31')
ON CONFLICT (policy_number) DO NOTHING;