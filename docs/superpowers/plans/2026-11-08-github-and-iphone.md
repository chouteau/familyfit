# GitHub and iPhone Implementation Plan

> **For agentic workers:** Execute sequentially with an independent final review.

**Goal:** Validate native iPhone code and prepare GitHub-based web image delivery.

**Architecture:** Existing .NET/SQLite/Docker site stays unchanged. GitHub Actions tests the web image before publishing to GHCR; a separate macOS job builds and tests iPhone code without signing. VPS execution stays pending the user's choice of deployment mechanism.

**Tech Stack:** GitHub Actions, GHCR, Docker Compose, Xcode/SwiftUI.

**Spec:** User correction on 2026-11-08: Mac is `marc@192.168.10.33`; site deployment goes through GitHub. Existing requirements are in `docs/implementation.md`.

## Global Constraints

- No credentials, health database or provisioning profiles in Git or CI artifacts.
- Preserve root `assets/`, which is unrelated pre-existing local work.
- Production image publishes only from main or a version tag, after successful web tests and container smoke checks.
- iPhone signing and real-device validation remain separate from unsigned simulator tests.

## Review Focus

- Pull requests cannot publish packages or access deployment secrets.
- Production image rejects anonymous family access and has demo disabled.
- Registry Compose preserves the same persistent volume and requires an explicit image tag.
- Mac SSH failures do not trigger security changes or password exposure.
- Publishing an image does not silently deploy or migrate a real VPS database.

## Tasks

- [x] Inspect SSH access, repository remote and current workflows. Mac responds but public-key authentication is not yet authorized; request user setup.
- [ ] Add pinned GitHub Actions for .NET/SQLite tests, Docker smoke checks and GHCR publishing; add independent unsigned iPhone simulator test job.
- [ ] Provide registry-only Compose and deployment instructions; validate YAML and Compose without printing resolved secrets.
- [ ] Publish the completed application and workflow on a review branch/PR, preserve unrelated files and observe CI.
- [ ] Correct native compiler/test failures from an actual Xcode run, then update validation evidence.
- [ ] Obtain an independent final technical review of this delivery change.
