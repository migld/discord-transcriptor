-- Placeholder NPCs for Phase 1 testing.
-- Replace names, personalities, and speech_patterns with your actual friend group.

INSERT INTO npcs (name, avatar_url, personality, speech_patterns) VALUES
(
  'Alex',
  NULL,
  'Sarcastic and quick-witted. Always has a comeback ready and rarely takes anything seriously. Laughs off tense situations but gets visibly uncomfortable when cornered.',
  ARRAY['uses dry humour constantly', 'deflects with jokes', 'says "obviously" a lot', 'raises one eyebrow when skeptical']
),
(
  'Jordan',
  NULL,
  'The overly-analytical one. Breaks everything down into logic and probabilities. Gets flustered when emotions enter the picture. Secretly very loyal.',
  ARRAY['speaks in lists', 'says "statistically speaking"', 'quotes things they read somewhere', 'pauses before answering like they''re computing']
),
(
  'Morgan',
  NULL,
  'The social chameleon — charming and likeable on the surface, but evasive about personal details. Remembers everyone''s business except their own.',
  ARRAY['deflects personal questions', 'very warm opener then vague closer', 'says "between us..."', 'laughs to buy time']
),
(
  'Riley',
  NULL,
  'High-strung and anxious. Talks fast, second-guesses themselves mid-sentence. Deeply honest but terrible at hiding stress. Has an alibi for everything — possibly too many.',
  ARRAY['talks over themselves', 'says "wait no I meant—"', 'always argues even when agreeing', 'nervous laugh']
);
