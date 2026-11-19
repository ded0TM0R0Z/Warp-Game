# CATALOG SEARCH AND DOWNLOAD CONTROL

## Catalog search

The catalog search field filters the already loaded catalog locally. It matches:
- Game name
- Description
- Engine identifier
- Game type
- Permanent game ID

Search does not require a server request and works with the cached catalog while offline. An active download remains visible even when the current search text would otherwise filter its game out.

## Download progress

The catalog displays percentage progress while package bytes are received. After the complete package is downloaded, its SHA-256 and size are checked and the UI changes to **Проверка и установка…**. Cancellation is disabled at that point because the package is being installed transactionally.

## Cancellation

- The cancel action is available only while the network transfer is active.
- Cancellation disconnects the active HTTPS connection.
- The input stream checks the cancellation token while reading.
- The installer removes its temporary package and staging directory on failure.
- A cancelled download must not create an installed-game record or replace an existing version.

## Automatic retry

The downloader makes at most three attempts total: the initial attempt and up to two retries.

Retryable conditions:
- HTTP 408
- HTTP 429
- HTTP 5xx
- Connection reset, timeout, DNS/connect failures, TLS connection failures and unexpected end-of-stream errors

Non-retryable conditions include invalid URLs, other HTTP 4xx responses, checksum mismatch, manifest mismatch, unsupported engine versions and package validation failures.

Retry delays increase from 750 ms to 1,500 ms. Progress resets for each new attempt.

## Not implemented

- Pause/resume of partially downloaded packages
- Background download service across process termination
- Download queue for multiple simultaneous packages

## Device acceptance tests

1. Search by a full game name, partial name, engine and game type.
2. Search for a string with no matches.
3. Start a package download and cancel before completion.
4. Confirm the UI reports cancellation and the game is not installed.
5. Start a download against a temporarily unavailable release and confirm retry attempts.
6. Confirm checksum or manifest failures are not retried.
7. Confirm cancellation disappears when verification and installation begin.
8. Confirm an update failure preserves the previously installed version.
