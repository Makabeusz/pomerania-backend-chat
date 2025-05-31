CREATE KEYSPACE IF NOT EXISTS messages WITH replication = {'class': 'SimpleStrategy', 'limit': 1};

-- Messages by room
CREATE TABLE messages.messages (
    room_id TEXT,
    created_at TIMESTAMP,
    message_id TEXT,
    profile_id VARCHAR,
    username VARCHAR,
    recipient_profile_id VARCHAR,
    recipient_username VARCHAR,
    content TEXT,
    message_type TEXT,
    resource_id TEXT,
    thread_id TEXT,
    edited_at VARCHAR,
    deleted_at TEXT,
    pinned BOOLEAN,
    metadata MAP<TEXT, TEXT>,
    PRIMARY KEY (room_id, created_at, message_id))
WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);

-- Reactions
CREATE TABLE messages.reactions (
    message_id TEXT,
    content TEXT,
    profile_id VARCHAR,
    created_at TIMESTAMP,
    PRIMARY KEY (message_id),
);

-- Read receipts
CREATE TABLE messages.read_reactions (
    id TEXT,
    profile_id VARCHAR,
    username TEXT,
    read_at TIMESTAMP,
    content TEXT,
    PRIMARY KEY (id, profile_id)
);

-- Group memberships
CREATE TABLE messages.group_members (
    group_id TEXT,
    profile_id VARCHAR,
    joined_at TIMESTAMP,
    role TEXT,
    content TEXT,
    PRIMARY KEY (group_id, profile_id)
);

-- Typing indicators
CREATE TABLE messages.typing_indicators (
    room_id TEXT,
    profile_id TEXT,
    username TEXT,
    last_typed_at TIMESTAMP,
    content TEXT,
    PRIMARY KEY (room_id, profile_id)
);

-- Moderation actions
CREATE TABLE messages.actions (
    action_id VARCHAR,
    message_id TEXT,
    profile_id TEXT,
    room_id TEXT,
    action_type TEXT,
    reason TEXT,
    moderator_id TEXT,
    created_at TIMESTAMP,
    content TEXT,
    PRIMARY KEY (room_id, action_type, created_at)
) WITH CLUSTERING ORDER BY (action_type DESC, created_at DESC);

-- Message search index (future use)
CREATE TABLE messages.search (
    room_id TEXT,
    content_token TEXT,
    message_id TEXT,
    created_at TIMESTAMP,
    profile_id TEXT,
    content TEXT,
    PRIMARY KEY ((room_id, content_token), created_at, message_id)
) WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);