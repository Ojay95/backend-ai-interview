-- Flyway baseline migration: Add performance index for interview sessions
CREATE INDEX IF NOT EXISTS idx_interview_sessions_user ON interview_sessions (user_id);
