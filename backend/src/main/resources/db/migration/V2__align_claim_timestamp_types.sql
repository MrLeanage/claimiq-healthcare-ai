-- Forward-only correction: preserve V1 and its checksum for existing databases.
-- Legacy datetime2 timestamps represent UTC; SQL Server adds the +00:00 offset
-- when converting datetime2 to datetimeoffset. No wall-clock adjustment is made.
DROP INDEX ix_claims_created_at ON claims;
ALTER TABLE claims ALTER COLUMN created_at datetimeoffset(7) NOT NULL;
ALTER TABLE claims ALTER COLUMN updated_at datetimeoffset(7) NOT NULL;
ALTER TABLE claims ALTER COLUMN submitted_at datetimeoffset(7) NULL;
CREATE INDEX ix_claims_created_at ON claims(created_at DESC);
