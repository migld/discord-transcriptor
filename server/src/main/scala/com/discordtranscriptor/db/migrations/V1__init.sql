-- Users (populated via Discord OAuth)
CREATE TABLE users (
    id           VARCHAR(32) PRIMARY KEY,   -- Discord user ID
    username     VARCHAR(100) NOT NULL,
    avatar       VARCHAR(200),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- NPC characters (friends as suspects)
-- Manually seeded for Phase 1; transcript-trained in Phase 2
CREATE TABLE npcs (
    id                  SERIAL PRIMARY KEY,
    discord_user_id     VARCHAR(32) REFERENCES users(id),
    name                VARCHAR(100) NOT NULL,
    avatar_url          VARCHAR(200),
    personality         TEXT NOT NULL,       -- free-text persona description
    speech_patterns     TEXT[],              -- e.g. ARRAY['says bro a lot', 'uses gaming refs']
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Game sessions
CREATE TABLE game_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id       VARCHAR(32) NOT NULL REFERENCES users(id),
    victim_npc_id   INTEGER NOT NULL REFERENCES npcs(id),
    killer_npc_id   INTEGER NOT NULL REFERENCES npcs(id),
    scenario        JSONB NOT NULL,          -- generated scene, motives, alibis
    status          VARCHAR(20) NOT NULL DEFAULT 'active',  -- active | accused | solved
    accused_npc_id  INTEGER REFERENCES npcs(id),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at        TIMESTAMPTZ,

    -- Multiplayer-ready: room_id and players[] unused in Phase 1
    room_id         UUID,
    player_ids      VARCHAR(32)[] NOT NULL DEFAULT ARRAY[]::VARCHAR[]
);

-- Individual messages within a game session
CREATE TABLE game_messages (
    id          SERIAL PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES game_sessions(id) ON DELETE CASCADE,
    sender      VARCHAR(20) NOT NULL,        -- 'player' | 'npc'
    npc_id      INTEGER REFERENCES npcs(id),
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Clues discovered by the player
CREATE TABLE clues (
    id          SERIAL PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES game_sessions(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    location    VARCHAR(100),
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_game_sessions_player ON game_sessions(player_id);
CREATE INDEX idx_game_messages_session ON game_messages(session_id);
CREATE INDEX idx_clues_session ON clues(session_id);
