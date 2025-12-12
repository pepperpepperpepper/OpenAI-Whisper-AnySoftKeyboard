# F-Droid Publishing & Workflow (Canonical)
# Single source of truth — do not follow other scattered notes; keep this file updated.

## Goals
- Prevent accidental loss/pruning of APKs and indexes.
- Keep all desired historical builds visible under “Other versions”.
- Make publishing deterministic, scripted, and recoverable.

## Bucket & Retention
- Enable S3 **Versioning** and **MFA delete** on `fdroid-uh-oh-wtf`.
- Add lifecycle rule: expire non-current object versions after 180–365 days; never expire current.
- Before each deploy, refuse to run if versioning is disabled.

## Metadata Strategy
- Use `UpdateCheckMode: Static` and `ArchivePolicy: KeepAll` in `metadata/wtf.uhoh.newsoftkeyboard.yml`.
- Generate metadata from the actual APK inventory via a script (no manual edits). Store script at `scripts/fdroid/generate_metadata.py`.
- Script logic: scan repo+archive for `wtf.uhoh.newsoftkeyboard_*.apk`, extract versionCode/versionName with `aapt`, emit a build block per APK, set `CurrentVersion/Code` to the highest code.
- Keep `AutoUpdateMode: None`.

## Safer Deploy Flow
1) Sync **from S3 to staging**: `aws s3 sync s3://fdroid-uh-oh-wtf/repo/ repo/` and same for `archive/`.
2) Run metadata generator on staging files.
3) Run `fdroid update --create-metadata` in a temp dir (not in live repo).
4) Validate counts:
   - Total APKs found >= expected_min (configurable, e.g., 7 or 40).
   - CurrentVersion matches build.gradle override.
5) If valid, sync staging **back to S3** (repo + archive + indexes) and invalidate CloudFront.
6) Before writing to S3, create a tarball backup of repo+archive+metadata with a timestamp under `backups/`.

## CI/Checks
- Add a CI job that:
  - Runs metadata generator.
  - Runs `fdroid update` in temp dir.
  - Asserts “Other versions” count >= threshold and no APK hash changes.
  - Fails if S3 versioning is off or APK count drops.

## Keep List
- Maintain `scripts/fdroid/keep_apks.txt` with versionCodes that must remain indexed. Deploy script aborts if any are missing locally or in S3.

## Operational Guardrails
- Never run `fdroid update` directly on the live repo; always via staging script.
- Require `AWS_PROFILE` or env vars and a confirmation prompt before pushing to S3.
- Log the list of APKs being indexed and the resulting CurrentVersion/Code each run.

## Optional
- Split channels: keep latest N in `repo/`, rest in `archive/`, but index all via `KeepAll` so clients can downgrade without physical moves.
- Add proper repo/archive icons to silence warnings.

## Immediate Actions (if approved)
- Turn on S3 versioning + MFA delete.
- Add generator script + backup/staging deploy script.
- Regenerate metadata from current APK inventory; set CurrentVersion/Code to desired release.
- Redeploy via staging with CloudFront invalidation.

## One-Command Publish (canonical)
- Script: `fdroid/scripts/publish.sh`
- Prereqs: `fdroid/.env` (copy from `.env.example`), `aapt`, `aws`, `/home/arch/fdroid-env/bin/fdroid`.
- Data dir default: `FDROID_DATA=/home/arch/fdroid` (overridable).
- Flow (automated):
  1) source `.env`, guard envs
  2) sync from S3 (repo/archive)
  3) bump versionCode/versionName (unless `SKIP_BUMP=1`)
  4) build signed release
  5) stage APK into repo/, regenerate metadata from inventory
  6) run `fdroid update --create-metadata`
  7) validate APK count threshold
  8) backup repo+archive+metadata
  9) sync back to S3, invalidate CloudFront
  10) optional git commit (skip with `SKIP_COMMIT=1`)

## Keystore / Signing Reference
- Keystore file: `fdroid/keystore.jks`
- Alias: `fdroidrepo` (matches `repo_keyalias` in `config.yml`)
- Environment variables expected by `config.yml`:
  - `FDROID_KEYSTORE_PASS` (store password)
  - `FDROID_KEY_PASS` (alias/key password)
  - AWS / bucket: `FDROID_AWS_BUCKET`, `FDROID_AWS_ACCESS_KEY_ID`, `FDROID_AWS_SECRET_KEY`
- Guard step before running `fdroid update`:
  - `fdroid/scripts/fdroid/check_keystore_env.sh` verifies these envs are set and non-empty; run it before any update/deploy.
- Local secret storage:
  - Real values live in `fdroid/.env` (git-ignored); source it with `set -a && source fdroid/.env && set +a`.
  - `fdroid/.env.example` lists required keys for new machines.
