# Discord Transcriptor

A murder mystery web game where NPC characters are based on real members of a Discord server. Players interrogate suspects (their friends), collect clues, and make an accusation. NPCs respond using AI personas built from hand-written personality descriptions.

## Prerequisites

- [Docker](https://www.docker.com/) + Docker Compose
- A [Discord application](https://discord.com/developers/applications) (for OAuth)
- A [Claude API key](https://console.anthropic.com)

## Getting Started

**1. Clone the repo**

```bash
git clone https://github.com/migld/discord-transcriptor.git
cd discord-transcriptor
```

**2. Set up environment variables**

```bash
cp .env.example .env
```

Edit `.env` and fill in:

| Variable | Where to get it |
|---|---|
| `JWT_SECRET` | Any long random string (`openssl rand -hex 32`) |
| `DISCORD_CLIENT_ID` | Discord Developer Portal → Your App → OAuth2 |
| `DISCORD_CLIENT_SECRET` | Same page |
| `DISCORD_REDIRECT_URI` | Set to `http://localhost:3000/auth/callback` for local dev |
| `CLAUDE_API_KEY` | [console.anthropic.com](https://console.anthropic.com) |

In your Discord app's OAuth2 settings, add `http://localhost:3000/auth/callback` as a redirect URI.

**3. Start the app**

```bash
docker-compose up --build
```

- Frontend: http://localhost:3000
- API: http://localhost:8080

**4. Seed NPCs**

Before starting a game, add at least 2 NPCs to the database. Edit `server/src/main/scala/com/discordtranscriptor/db/migrations/V2__seed_npcs.sql` with your friends' personas, then rebuild.

## Tech Stack

- **API** — Scala 2.13 + Akka HTTP
- **Database** — PostgreSQL 16 (via Slick)
- **AI** — Claude API (`claude-sonnet-4-6`)
- **Frontend** — React 18 + Vite
- **Auth** — Discord OAuth2 + JWT
