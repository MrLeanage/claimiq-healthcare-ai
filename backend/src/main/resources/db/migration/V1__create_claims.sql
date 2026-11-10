CREATE TABLE claims (
 id uniqueidentifier NOT NULL PRIMARY KEY,
 claim_number varchar(40) NOT NULL UNIQUE,
 patient_reference varchar(100) NOT NULL,
 provider_reference varchar(100) NOT NULL,
 amount decimal(18,2) NOT NULL,
 currency varchar(3) NOT NULL,
 service_date date NOT NULL,
 status varchar(20) NOT NULL,
 created_at datetime2(6) NOT NULL,
 updated_at datetime2(6) NOT NULL,
 submitted_at datetime2(6) NULL,
 version bigint NOT NULL DEFAULT 0,
 CONSTRAINT ck_claim_amount CHECK (amount > 0),
 CONSTRAINT ck_claim_status CHECK (status IN ('DRAFT','SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED'))
);
CREATE INDEX ix_claims_created_at ON claims(created_at DESC);
CREATE INDEX ix_claims_status ON claims(status);