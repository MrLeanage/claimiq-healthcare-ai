-- The reservation, claim and CREATE audit event commit together.
CREATE TABLE claim_creation_requests (
 id uniqueidentifier NOT NULL PRIMARY KEY,
 owner_subject nvarchar(255) COLLATE Latin1_General_100_BIN2 NOT NULL,
 request_key uniqueidentifier NOT NULL,
 request_hash varchar(64) NOT NULL,
 claim_id uniqueidentifier NULL,
 created_at datetimeoffset(7) NOT NULL,
 CONSTRAINT uq_claim_creation_request UNIQUE (owner_subject, request_key),
 CONSTRAINT fk_claim_creation_request_claim FOREIGN KEY (claim_id) REFERENCES claims(id)
);
