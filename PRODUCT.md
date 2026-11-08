# FamilyFit
<!-- impeccable:product-schema 1 -->

## Platform
adaptive

## Stack
.NET 10 Blazor SSR/Interactive Server, EF Core SQLite, Android Kotlin/Compose, iOS SwiftUI. Single Docker application instance on fit.chouteau.info.

## Users
One private family. Each member has their own phone, profile, goals, schedule and equipment. The family administrator manages invitations and approves programs.

## Product Purpose
Help everyone exercise with their own equipment and make the family's actual progress visible without treating rest as a missed workout.

## Capabilities and Constraints
No usernames/passwords. A one-time `FAMILYFIT_BOOTSTRAP_CODE` from the Compose environment opens the first administrator session and creates the profile when needed. Once initialized, anonymous web pages stay blank; an administrator creates a member-specific web link valid for five minutes, then the matching enrolled phone approves its QR/code. Personal one-time invitations enroll phones. All members may read family profiles including age and weight; only owners/admin edit. External agents use a dedicated MCP key supplied by the server environment (`MCP_API_KEY`); domain changes use ChannelMediator, and program publication still requires administrator approval. No embedded web AI generator remains. Downloaded mobile workouts work offline and synchronize idempotently. Web is for planning and family tracking, mobile for workout execution.

## Brand Commitments
Approved home and member planning compositions: warm ivory, navy rounded typography, leaf green, orange and blue, athletic seagull, generous buttons, labeled statuses. Joyful, supportive, accessible. Illustrative data must be labeled and never mixed with production records.

## Evidence on Hand
Existing Android coach, exercise images/videos and seagull; two approved generated screen mockups in the conversation. No production member records or real completed workouts supplied.
