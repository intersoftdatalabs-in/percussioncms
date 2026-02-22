# Plan: Installer DB Target Enhancement (8.2)

## Decision

Treat this as a blocker for “fully reliable non-Derby install + verify” workflows.

## Workstreams

### 1) Installer input contract

Define a single contract for fresh install DB targeting, for example:

- DB target (`DERBY|MYSQL|MSSQL|ORACLE`)
- Host, port, database/service name, schema
- Username/password
- Driver + dialect defaults per backend

Deliverable:

- One documented mapping table of input -> config files/fields.

### 2) CMS installer write-through

Ensure fresh install writes selected backend to repository config before DB setup steps execute.

Deliverable:

- Fresh install path produces backend-correct `rxrepository.properties` values.

### 3) DTS installer write-through

Ensure fresh install writes backend-correct values into DTS datasource config.

Deliverable:

- Fresh install path produces backend-correct `perc-datasources.properties` values.

### 4) Connectivity validation and fail-fast

Add pre-flight DB connection check and clear errors.

Deliverable:

- Installer stops early with explicit reason and remediation hints.

### 5) Agent/dev verification contract

Expose one command that reports effective backend and key connection fields from running install.

Deliverable:

- Machine-readable pass/fail + detailed log path.

### 6) Test matrix

At minimum:

- Derby fresh install
- MySQL fresh install
- Upgrade preserving existing backend

## Sequencing recommendation

1. Finalize input contract.
2. Implement CMS/DTS fresh install write-through.
3. Add validation and verification output.
4. Add tests and docs.
5. Re-run docker workflow task against new installer behavior.

## Exit criteria for blocker removal

- Fresh install to MySQL works without manual file edits.
- Verification command confirms selected backend in both CMS and DTS configs.
- Existing upgrade behavior remains unchanged.
