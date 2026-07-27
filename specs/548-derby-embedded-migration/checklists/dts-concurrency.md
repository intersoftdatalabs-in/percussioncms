# DTS concurrent write smoke (T071 / SC-005)

|            Item            |                                     Result                                     |
|----------------------------|--------------------------------------------------------------------------------|
| Harness                    | `PSH2DtsConcurrentWriteSmokeTest`                                              |
| Model                      | Metadata-like `METADATA_PAGE` upserts (path+site unique), multiuser H2 file DB |
| Concurrency                | 8 writers × 25 writes = 200 rows                                               |
| Outcome                    | Full row count, no null payloads, 0 silent loss (automated)                    |
| Full Tomcat DTS stack soak | Deferred to post-merge install validation if needed                            |

CMS editor floor evidence: `PSH2MultiuserLockHarnessTest` (T009/T069/T070).
