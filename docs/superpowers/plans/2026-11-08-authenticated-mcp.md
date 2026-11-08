# FamilyFit Authenticated MCP Implementation Plan

**Goal:** Replace embedded AI with authenticated agent tools and personal five-minute web login links.

**Architecture:** Environment-backed MCP key, separate agent actor, bounded reads and existing ChannelMediator commands. Private member login links use an additive SQLite migration. No agent key database or key-management page.

**Tech Stack:** .NET 10, Blazor Server/SSR, EF SQLite, ChannelMediator, ModelContextProtocol.AspNetCore 2.4.0.

**Spec:** `docs/superpowers/specs/2026-11-08-authenticated-mcp-design.md`.

## Constraints and review focus

- Preserve mobile/browser authorization and historical SQL query shapes.
- `MCP_API_KEY` belongs in the production Compose `.env`; absent means disabled. Never log it.
- Validate current sole administrator at every call; reject mobile/cookie credentials at MCP.
- Agents cannot create access or approve login; `MCP_READ_ONLY=true` cannot mutate.
- Private link lifetime is exactly five minutes from creation; only its expected member can approve it.
- After initial administrator setup, anonymous login remains empty, family visibility after login remains shared.
- Published programs/results and UUID retry rules remain unchanged.

## Tasks

- [x] Contracts/Core: separate agent authorization and human-only markers; bounded MCP query handler; member login link entity, migration, handlers and extensions.
- [x] Web: remove embedded AI service/request/button/endpoint, add private member link panel and blank public login; operator first-login command.
- [x] MCP: SDK stateless transport, bearer policy, origin validation, 18 domain tools through mediator.
- [x] Configure all three Compose variants with `MCP_API_KEY` and `MCP_READ_ONLY`; document client environment-variable setup, tool catalogue and private web login.
- [x] Validate real MCP and private-link tests, inspect desktop/mobile, build production container and verify startup/authentication.
- [ ] Independent whole-change review, correct concrete findings, publish reviewable PR and observe CI. No production deployment or secret creation.

- [x] Ajouter le code initial FAMILYFIT_BOOTSTRAP_CODE à usage unique, formulaire web, état SQLite, session sans Device, tests concurrence/rejeu/installations existantes et README demandé.
- [x] Vérifier visuellement le formulaire initial et son parcours complet, puis reconstruire Docker et vérifier l’état consommé après redémarrage.
