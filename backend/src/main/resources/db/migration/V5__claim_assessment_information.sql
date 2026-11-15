-- Existing claims remain valid historical records with no invented clinical data.
ALTER TABLE claims ADD payer_name nvarchar(150) NULL, plan_reference nvarchar(100) NULL,
 member_reference nvarchar(100) NULL, coverage_status varchar(20) NULL,
 coverage_verified_on date NULL, authorization_status varchar(20) NULL,
 authorization_reference nvarchar(100) NULL, clinical_summary nvarchar(2000) NULL;
CREATE TABLE claim_diagnoses (
 claim_id uniqueidentifier NOT NULL,
 diagnosis_order int NOT NULL,
 diagnosis_code varchar(20) NOT NULL,
 CONSTRAINT pk_claim_diagnoses PRIMARY KEY (claim_id,diagnosis_order),
 CONSTRAINT fk_diagnoses_claim FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE
);
CREATE TABLE claim_service_lines (
 claim_id uniqueidentifier NOT NULL,
 line_number int NOT NULL,
 code_system varchar(20) NOT NULL,
 procedure_code varchar(20) NOT NULL,
 service_description nvarchar(200) NOT NULL,
 units int NOT NULL,
 unit_price decimal(18,2) NOT NULL,
 CONSTRAINT pk_claim_service_lines PRIMARY KEY (claim_id,line_number),
 CONSTRAINT fk_service_lines_claim FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
 CONSTRAINT ck_service_line_units CHECK (units BETWEEN 1 AND 999),
 CONSTRAINT ck_service_line_price CHECK (unit_price > 0),
 CONSTRAINT ck_service_code_system CHECK (code_system IN ('LOCAL','CPT','HCPCS'))
);
