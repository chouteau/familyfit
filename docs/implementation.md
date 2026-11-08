# FamilyFit — implementation approved 2026-11-08

## Goal
Private family dashboard and individual weekly planning faithful to the approved mockups; synchronized native mobile coaches.

## Decisions
.NET 10 Blazor SSR/Interactive Server, EF Core 10 SQLite/WAL on a local persistent Docker volume; one server replica. Kotlin Android preserved; SwiftUI iOS 18+, private registered-device distribution. No passwords. Personal invitations (10 minutes) enroll devices, mobile approval of browser challenge (5 minutes) creates a 12-hour revocable browser session. Recovery is an operator-only CLI command. Profiles and equipment are centrally managed; downloaded workouts/results are locally durable. Administrator alone publishes programs, including AI drafts. All family members can read profiles and progress.

## Implementation sequence
- [x] Database, authorization, invitations, browser approval, revocation and recovery.
- [x] Shared catalogue, member/location CRUD, immutable published programs, workout submission and coherent sync snapshots.
- [x] Blazor dashboard/planning, management forms, AI proposal workflow and accessibility.
- [x] Android secure enrollment, QR approval, cached programs and durable outbox connected to coach.
- [x] SwiftUI app source, secure enrollment, camera scanner, local coach, offline storage and sync.
- [x] Docker, backup/recovery instructions, server/API tests, Android build and browser checks.
- [ ] Compile/sign/test the iPhone app on the user's Mac and physical devices.
- [ ] Run Android instrumentation tests on a connected phone/emulator.
- [ ] Deploy to the user's VPS, configure HTTPS and validate actual mobile-to-server flows.

## Verification focus
One-time and expired challenges; stolen browser code cannot redeem session; device revocation invalidates web access; cross-member mutation forbidden; concurrent duplicate results counted once; partial/rest classification; local dates through Europe/Paris DST; interrupted local workout; SQLite persistence and consistent backup. iOS compilation and device/signature validation require the user's Mac and Apple Developer account; do not report these as tested from Windows.

See `verification.md` for the checks actually run and their limitations.
