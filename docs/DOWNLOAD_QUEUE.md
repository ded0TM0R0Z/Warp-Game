# DOWNLOAD QUEUE

## Behavior

- Catalog installs are added to a persistent local queue.
- Only one package is downloaded and installed at a time.
- Additional games can be queued while a download is active.
- Queue entries retain the complete catalog metadata required to download after the catalog is unavailable.
- Queue state is stored in application-private preferences and survives normal process restarts.
- Pending entries resume automatically when the app starts.
- An active transfer is not resumed after process termination. It is marked failed; pending entries remain queued.
- A failed entry can be retried manually.
- Pending, completed, failed and cancelled entries can be removed from the queue. An active transfer must first be cancelled.
- Duplicate game IDs cannot be queued simultaneously.

## State model

- `pending`: waiting for its turn.
- `downloading`: active network transfer or package installation.
- `completed`: package was verified and installed.
- `failed`: transfer or installation failed, or the process stopped during an active transfer.
- `cancelled`: user cancelled the active transfer.

Cancellation remains available only during network transfer. Once package verification succeeds and transactional installation starts, cancellation is disabled.

## Integrity and recovery

The queue does not bypass the existing installer. Every package still passes the configured HTTPS host checks, size checks, SHA-256 verification, archive validation, manifest validation and atomic installation.

Partial package files are not preserved as resumable downloads. A process interruption is recorded as a failure and a retry starts a fresh transfer.

## Known limits

- Queue execution is tied to the app process; it is not an Android background service.
- Downloads do not continue after the app process is terminated.
- Pause/resume is not implemented.
- Queue records are local to this app installation and are not synchronized across devices.
- A successful cloud build does not replace real-device testing of restart recovery, cancellation and storage behavior.

## Device acceptance tests

1. Add multiple games and verify they download one at a time.
2. Add another game while a transfer is active.
3. Navigate away from Catalog and confirm progress remains visible in Downloads.
4. Cancel an active transfer and confirm the entry becomes cancelled and no game is installed from that transfer.
5. Close the app during a transfer, relaunch, and confirm the interrupted entry is failed while pending entries remain queued.
6. Retry a failed entry.
7. Remove pending, completed, failed and cancelled entries.
8. Confirm duplicate game IDs cannot be added twice.
9. Confirm an installed update failure preserves the previous installed version.
