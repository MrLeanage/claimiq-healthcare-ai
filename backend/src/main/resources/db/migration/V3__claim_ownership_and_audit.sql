-- Historical claims intentionally have no owner until explicitly reconciled.
-- They are visible to reviewers only; do not assign them to arbitrary new users.
ALTER TABLE claims ADD owner_subject varchar(255) NULL;
CREATE INDEX ix_claims_owner_created ON claims(owner_subject, created_at DESC, id DESC);
CREATE TABLE claim_audit_events (
 id uniqueidentifier NOT NULL PRIMARY KEY,
 claim_id uniqueidentifier NOT NULL,
 actor_subject varchar(255) NOT NULL,
 action varchar(32) NOT NULL,
 from_status varchar(20) NULL,
 to_status varchar(20) NOT NULL,
 claim_version bigint NOT NULL,
 occurred_at datetimeoffset(7) NOT NULL,
 CONSTRAINT fk_claim_audit_claim FOREIGN KEY (claim_id) REFERENCES claims(id)
);
CREATE INDEX ix_claim_audit_claim ON claim_audit_events(claim_id, occurred_at);
