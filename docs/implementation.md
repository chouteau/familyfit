# FamilyFit — implementation approved 2026-11-08

## Goal
Private family dashboard and individual weekly planning faithful to the approved mockups; synchronized native mobile coaches.

## Decisions
.NET 10 Blazor SSR/Interactive Server, EF Core 10 SQLite/WAL on a local persistent Docker volume; one server replica. Kotlin Android preserved; SwiftUI iOS 18+, private registered-device distribution. No passwords. Personal invitations (10 minutes) enroll devices. An administrator creates member-specific web links (5 minutes); the matching phone must approve their QR/code before a 12-hour revocable browser session is created. The first administrator session can instead use the one-time FAMILYFIT_BOOTSTRAP_CODE from Compose; afterwards public login is blank without a valid link. Recovery and first web link are operator CLI commands. Profiles and equipment are centrally managed; downloaded workouts/results are locally durable. Administrator alone publishes programs, prepared manually or by an external MCP agent. The separate MCP bearer key comes from `MCP_API_KEY` in the Compose environment, with optional `MCP_READ_ONLY`. All family members can read profiles and progress.

## Implementation sequence
- [x] Database, authorization, invitations, browser approval, revocation and recovery.
- [x] Shared catalogue, member/location CRUD, immutable published programs, workout submission and coherent sync snapshots.
- [x] Blazor dashboard/planning, management forms, private web links and accessibility; embedded AI removed in favor of authenticated MCP tools.
- [x] Android secure enrollment, QR approval, cached programs and durable outbox connected to coach.
- [x] SwiftUI app source, secure enrollment, camera scanner, local coach, offline storage and sync.
- [x] Docker, backup/recovery instructions, server/API tests, Android build and browser checks.
- [ ] Compile/sign/test the iPhone app on the user's Mac and physical devices.
- [x] Compile the iPhone app and run 3 XCTest on a GitHub macOS simulator runner.
- [ ] Run Android instrumentation tests on a connected phone/emulator.
- [ ] Deploy to the user's VPS, configure HTTPS and validate actual mobile-to-server flows.

## Verification focus
One-time and expired challenges; stolen browser code cannot redeem session; device revocation invalidates web access; cross-member mutation forbidden; concurrent duplicate results counted once; partial/rest classification; local dates through Europe/Paris DST; interrupted local workout; SQLite persistence and consistent backup. iOS compilation and device/signature validation require the user's Mac and Apple Developer account; do not report these as tested from Windows.

See `verification.md` for the checks actually run and their limitations.
