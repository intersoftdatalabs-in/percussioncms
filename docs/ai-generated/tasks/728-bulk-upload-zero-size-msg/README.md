# Bug Fix #728 – Bulk Upload Gadget: Queue Count Becomes Negative on Removing Failed or Rejected Files

## Problem Summary

- In the Bulk Upload Gadget, if the user uploads a zero-byte file, it is correctly rejected with the message `Cannot upload empty (0 byte) files`.
- However, if the user then clicks the close `[X]` icon to remove/dismiss the rejected file row from the queue, the upload status details display a negative queued file count (e.g. `-1 file(s) queued for upload` instead of `0 file(s) queued for upload`).
- The same issue happens if an actual file upload fails, and the user clicks `[X]` to dismiss the failed row.

## Root Cause

- In [perc_bulk_file_upload.js](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.gadget.bulkFileUpload/sys__UserDependency--cm/gadgets/repository/perc_bulk_file_upload_gadget/js/perc_bulk_file_upload.js):
  - When a file is added, `TOTAL_IN_QUEUE` is incremented, and `generateButtonHTML` is called to create the row and attach a click handler to the `[X]` close button.
  - If a file is a zero-byte file or if a file fails to upload, `TOTAL_IN_QUEUE` is decremented early to exclude it from the pending/queued count.
  - However, the click handler on `[X]` was unconditionally decrementing `TOTAL_IN_QUEUE` again when removing the row. This double-decrement caused the count to become negative (`-1`).

## Solution

- Modified the close button click handler in `generateButtonHTML` inside [perc_bulk_file_upload.js](file:///home/nate/projects/java8/percussioncms/system/Packages/perc.gadget.bulkFileUpload/sys__UserDependency--cm/gadgets/repository/perc_bulk_file_upload_gadget/js/perc_bulk_file_upload.js):
  - Check if the row has the `alert-danger` class (which is set on both early rejected zero-byte files and failed uploads).
  - Only decrement `TOTAL_IN_QUEUE` if the row does not have `alert-danger` (i.e. it is still a pending/queued file).
  - If the row has `alert-danger`, the queue count is not decremented, preventing the count from going negative.

## Validation

- Ran spotless check successfully.
- Re-built `perc-packages` module successfully, ensuring the `perc.gadget.bulkFileUpload` package compiles and packages correctly.
