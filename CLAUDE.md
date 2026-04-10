# Discord Transcriptor — Project Context

## What this is
A **murder mystery web game** where NPC characters are based on real members of a Discord server. Players interrogate suspects (their friends), collect clues, and make an accusation. The twist: NPCs respond using AI personas built from each person's real speech patterns.

## Build Phases

### Phase 1 — Current (Complete)
Game with **manually written NPC personas**. No voice recording, no transcription costs. Claude API powers NPC responses from hand-written personality descriptions.

### Phase 2 — Planned
Discord bot joins voice channels, records speech (opt-in only), transcribes via OpenAI Whisper. Transcripts stored per-user in PostgreSQL. Manual `/record start` / `/record stop` to control costs.

### Phase 3 — Planned
Replace hand-written personas with **transcript-trained personas**. Feed real user transcripts into Claude system prompts. Plug into existing game engine — no other changes needed.

### Multiplayer — Planned
`game_sessions` table already has `room_id` and `player_ids[]` columns ready. Not wired up yet.

## Tech Stack

| Layer | Tech |
|---|---|
| API | Scala 2.13 + Akka HTTP |
| DB access | Slick + HikariCP |
| JSON | Circe |
| HTTP client | sttp (calls Claude API + Discord OAuth) |
| Database | PostgreSQL 16 |
| Auth | Discord OAuth2 + JWT (stored in localStorage) |
| AI | Claude API (`claude-sonnet-4-6`) |
| Frontend | React 18 + Vite |
| Local dev | Docker Compose |

## Project Structure

```
discord-transcriptor/
├── CLAUDE.md
├── .env.example
├── docker-compose.yml
├── server/                          ← Scala Akka HTTP API (port 8080)
│   ├── build.sbt
│   ├── project/plugins.sbt          ← sbt-assembly for Docker build
│   ├── Dockerfile
│   └── src/main/scala/com/discordtranscriptor/
│       ├── Main.scala
│       ├── Server.scala             ← Akka HTTP server, CORS, route wiring
│       ├── db/
│       │   ├── Database.scala       ← Slick DB connection
│       │   ├── Tables.scala         ← Slick table definitions
│       │   └── migrations/V1__init.sql
│       ├── models/Models.scala      ← Case classes + Circe codecs
│       ├── services/
│       │   ├── AuthService.scala    ← Discord OAuth + JWT issue/validate
│       │   ├── GameEngine.scala     ← Scenario randomizer, session/clue CRUD
│       │   └── NpcEngine.scala      ← Claude API calls, clue extraction
│       └── routes/
│           ├── AuthRoutes.scala     ← /auth/login, /auth/callback, /auth/me
│           ├── GameRoutes.scala     ← /game/new, /game/:id, /talk, /accuse, /reveal
│           └── NpcRoutes.scala      ← /npcs
└── client/                          ← React + Vite (port 3000)
    ├── package.json
    ├── vite.config.js               ← proxies /api → localhost:8080
    ├── Dockerfile
    ├── nginx.conf
    └── src/
        ├── main.jsx, App.jsx
        ├── api.js                   ← axios client, all API calls
        ├── hooks/useAuth.js         ← JWT in localStorage
        └── pages/
            ├── Login.jsx/css        ← Discord OAuth login, noir splash
            ├── AuthCallback.jsx     ← grabs JWT from URL hash
            ├── Lobby.jsx/css        ← start new game
            ├── Game.jsx/css         ← main investigation screen
            ├── Accuse.jsx/css       ← final accusation
            └── Reveal.jsx/css       ← solution reveal

```

## Database Schema (PostgreSQL)

- `users` — Discord user_id, username, avatar (populated via OAuth)
- `npcs` — NPC characters with personality + speech_patterns (seeded manually in Phase 1)
- `game_sessions` — one per game, stores victim/killer IDs, scenario JSON, status
- `game_messages` — full conversation log per session, per NPC
- `clues` — clues discovered during a session
- Scenario data stored as JSONB inside `game_sessions.scenario`

## API Routes

```
GET  /auth/login              → redirect to Discord OAuth
GET  /auth/callback?code=...  → exchange code, issue JWT, redirect to frontend
GET  /auth/me                 → return current user (requires JWT)

GET  /npcs                    → list all NPCs

POST /game/new                → start game (randomize victim/killer/scenario)
GET  /game/:id                → session + suspects + messages + clues + scenario
POST /game/:id/talk           → { npcId, message } → Claude NPC response + clues
POST /game/:id/accuse         → { npcId } → correct/incorrect
GET  /game/:id/reveal         → killer, victim, motive, full story
```

## Environment Variables

Copy `.env.example` to `.env` and fill in:

```
JWT_SECRET               any long random string (openssl rand -hex 32)
DISCORD_CLIENT_ID        from discord.com/developers/applications
DISCORD_CLIENT_SECRET    same page
DISCORD_REDIRECT_URI     http://localhost:3000/auth/callback (local)
CLAUDE_API_KEY           from console.anthropic.com
```
DATABASE_URL is set automatically by docker-compose — do not set manually.

## Running Locally

```bash
cp .env.example .env   # fill in your keys
docker-compose up --build
# → client: http://localhost:3000
# → server: http://localhost:8080
```

## What Needs Doing Next

1. **Seed NPCs** — write a seed script or SQL insert for your friend group with hand-written personas. At least 2 NPCs required to start a game.
2. **Test the full flow** — login → new game → interrogate → accuse → reveal
3. **Tune NPC prompts** — adjust personality descriptions in the seed data to match your friends
4. **Production deployment** — Nginx reverse proxy config exists; needs a domain + SSL

## Key Design Decisions (from initial session)

- **Separate repos** for unrelated projects (accounting app, blog, resume). Monorepo only within discord-transcriptor (bot + server + client).
- **All projects can share one server** — Nginx/Caddy routes by subdomain, each service runs on its own port.
- **JWT over server-side sessions** — small friend group, no need to force-logout. Easy to swap later (auth is isolated to AuthService + AuthRoutes).
- **Persona prompting over fine-tuning** — feed transcripts as Claude context rather than training a model. Cheaper, faster, swappable.
- **Manual record toggle** — bot records only when `/record start` is called, minimizing Whisper API costs.
- **Opt-in consent** — voice recording and AI training requires explicit user opt-in. Build `/opt-in` and `/opt-out` commands into the bot.

## Deployment (Future)

```
yourdomain.com              → resume (static)
blog.yourdomain.com         → blog
accounting.yourdomain.com   → accounting app
game.yourdomain.com         → discord-transcriptor client (port 3000)
api.game.yourdomain.com     → discord-transcriptor server (port 8080)
```
All on one server behind Nginx/Caddy. API port is configurable via PORT env var.
