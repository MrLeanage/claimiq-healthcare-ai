# Investigating migration history mismatches

Flyway checks applied migration checksums before running newer migrations. A new
forward migration cannot, by itself, fix a mismatch in an already applied V1.

One existing development database was created with a V1 variant using
`datetimeoffset(7)` timestamps. Published V1 uses `datetime2(6)` and V2 converts
those columns. The current local V1 file was restored to match that database's
recorded checksum. This is local environment state, not a change to publish.
Preserve it until the database history has been deliberately reconciled.

## Read-only investigation

1. Confirm the intended SQL Server instance and database. A service reporting
   Running does not establish that the database is accepting queries.
2. Record the repository commit and `git diff -- backend/src/main/resources/db/migration/V1__create_claims.sql`.
3. Inspect migration history and the actual schema in that database:

   ```sql
   SELECT DB_NAME() AS database_name, @@SERVERNAME AS server_name;
   SELECT installed_rank, version, description, script, checksum, success
   FROM dbo.flyway_schema_history ORDER BY installed_rank;
   SELECT name, TYPE_NAME(user_type_id) AS data_type, scale, is_nullable
   FROM sys.columns
   WHERE object_id = OBJECT_ID('dbo.claims')
     AND name IN ('created_at', 'updated_at', 'submitted_at');
   ```

4. Compare the recorded migration files and schema with the published versions.
   A matching final column type alone is not proof that all migration effects match.
5. Keep a verified backup and test the proposed reconciliation on a restored copy
   before changing migration metadata in the original database. Record the exact
   before/after history, schema checks and validation result.

There is no automatic repair script here: a checksum mismatch does not establish
whether the database or the migration source is correct. Do not delete history,
reset the database, disable validation or run `repair` as a routine startup fix.

## Clean installation versus existing database

A clean checkout should create a new database using the published migration
sequence. An existing database must validate its applied history before upgrading.
Test both paths on disposable databases. SQL Server integration tests create a
separate `ClaimIQ_it_<uuid>` database and do not target the application's database.

V6 is a forward-only addition for creation retry reservations. It neither repairs
nor changes V1. Assessment results remain transient, and the model remains trained
on synthetic outcomes; neither capability is changed by migration reconciliation.
