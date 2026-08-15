# FastForward sample content

- `Config/Data/RxffSampleTableData.xml` — site folders, navons, pages, and
  inlined `ITEM` blobs for table-factory seed (`installSampleSites`).
- `importFiles/` — hashed binaries from the 7.3.2 FastForward tree
  (`*.jpg` / `*.gif` / `*.pdf` plus sibling `*.sha1`). Copied to
  `rxconfig/FastForward/importFiles` and registered in
  `autoImportBinaries.txt` so `PSDbStorageService` imports them on first
  start (same path as 7.3.2 `fastforwardSampleContent`).
