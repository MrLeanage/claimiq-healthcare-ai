-- JWT subjects are opaque, case-sensitive Unicode identifiers. Do not compare
-- them using a server's default case-insensitive or non-Unicode collation.
DROP INDEX ix_claims_owner_created ON claims;
ALTER TABLE claims ALTER COLUMN owner_subject nvarchar(255) COLLATE Latin1_General_100_BIN2 NULL;
CREATE INDEX ix_claims_owner_created ON claims(owner_subject, created_at DESC, id DESC);
ALTER TABLE claim_audit_events ALTER COLUMN actor_subject nvarchar(255) COLLATE Latin1_General_100_BIN2 NOT NULL;
