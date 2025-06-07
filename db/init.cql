CREATE KEYSPACE IF NOT EXISTS messages WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

-- Messages by room
CREATE TABLE IF NOT EXISTS messages.messages (
    room_id VARCHAR,
    created_at TIMESTAMP,
    profile_id VARCHAR,
    username VARCHAR,
    recipient_profile_id VARCHAR,
    recipient_username VARCHAR,
    content TEXT,
    message_type VARCHAR,
    resource_id VARCHAR,
    thread_id VARCHAR,
    edited_at VARCHAR,
    deleted_at VARCHAR,
    pinned BOOLEAN,
    read_at TIMESTAMP,
    metadata MAP<VARCHAR, VARCHAR>,
    PRIMARY KEY (room_id, created_at, profile_id))
WITH CLUSTERING ORDER BY (created_at DESC, profile_id ASC);

-- Read receipts (for group chats)
--CREATE TABLE IF NOT EXISTS messages.read_reactions (
--    id VARCHAR,
--    profile_id VARCHAR,
--    username VARCHAR,
--    read_at TIMESTAMP,
--    content VARCHAR,
--    PRIMARY KEY (id, profile_id)
--);

---- Reactions
--CREATE TABLE IF NOT EXISTS messages.reactions (
--    message_id VARCHAR,
--    content VARCHAR,
--    profile_id VARCHAR,
--    created_at TIMESTAMP,
--    PRIMARY KEY (message_id),
--);

-- Group memberships
--CREATE TABLE IF NOT EXISTS messages.group_members (
--    group_id VARCHAR,
--    profile_id VARCHAR,
--    joined_at TIMESTAMP,
--    role VARCHAR,
--    content VARCHAR,
--    PRIMARY KEY (group_id, profile_id)
--);
--
---- Typing indicators
--CREATE TABLE IF NOT EXISTS messages.typing_indicators (
--    room_id VARCHAR,
--    profile_id VARCHAR,
--    username VARCHAR,
--    last_typed_at TIMESTAMP,
--    content VARCHAR,
--    PRIMARY KEY (room_id, profile_id)
--);
--
---- Moderation actions
--CREATE TABLE IF NOT EXISTS messages.actions (
--    action_id VARCHAR,
--    message_id VARCHAR,
--    profile_id VARCHAR,
--    room_id VARCHAR,
--    action_type VARCHAR,
--    reason VARCHAR,
--    moderator_id VARCHAR,
--    created_at TIMESTAMP,
--    content VARCHAR,
--    PRIMARY KEY (room_id, action_type, created_at)
--) WITH CLUSTERING ORDER BY (action_type DESC, created_at DESC);
--
---- Message search index (future use)
--CREATE TABLE IF NOT EXISTS messages.search (
--    room_id VARCHAR,
--    content_token VARCHAR,
--    message_id VARCHAR,
--    created_at TIMESTAMP,
--    profile_id VARCHAR,
--    content VARCHAR,
--    PRIMARY KEY ((room_id, content_token), created_at, message_id)
--) WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);