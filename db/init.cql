CREATE KEYSPACE IF NOT EXISTS messages WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

-- Messages by room
CREATE TABLE IF NOT EXISTS messages.messages (
    room_id VARCHAR,
    created_at TIMESTAMP,
    message_id VARCHAR,
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
    metadata MAP<VARCHAR, VARCHAR>,
    PRIMARY KEY (room_id, created_at, message_id))
WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);

-- Conversations list
CREATE TABLE IF NOT EXISTS messages.conversations (
    user_id VARCHAR,
    last_message_at TIMESTAMP,
    room_id VARCHAR,
    other_user_id VARCHAR,
    other_username VARCHAR,
    last_message_content TEXT,
    PRIMARY KEY (user_id, last_message_at, room_id)
) WITH CLUSTERING ORDER BY (last_message_at DESC, room_id ASC);

CREATE TABLE IF NOT EXISTS messages.conversations_index (
    user_id VARCHAR,
    room_id VARCHAR,
    last_message_at TIMESTAMP,
    PRIMARY KEY (user_id, room_id)
) WITH CLUSTERING ORDER BY (room_id ASC);

-- Reactions
CREATE TABLE IF NOT EXISTS messages.reactions (
    message_id VARCHAR,
    content VARCHAR,
    profile_id VARCHAR,
    created_at TIMESTAMP,
    PRIMARY KEY (message_id),
);

-- Read receipts
CREATE TABLE IF NOT EXISTS messages.read_reactions (
    id VARCHAR,
    profile_id VARCHAR,
    username VARCHAR,
    read_at TIMESTAMP,
    content VARCHAR,
    PRIMARY KEY (id, profile_id)
);

-- Group memberships
CREATE TABLE IF NOT EXISTS messages.group_members (
    group_id VARCHAR,
    profile_id VARCHAR,
    joined_at TIMESTAMP,
    role VARCHAR,
    content VARCHAR,
    PRIMARY KEY (group_id, profile_id)
);

-- Typing indicators
CREATE TABLE IF NOT EXISTS messages.typing_indicators (
    room_id VARCHAR,
    profile_id VARCHAR,
    username VARCHAR,
    last_typed_at TIMESTAMP,
    content VARCHAR,
    PRIMARY KEY (room_id, profile_id)
);

-- Moderation actions
CREATE TABLE IF NOT EXISTS messages.actions (
    action_id VARCHAR,
    message_id VARCHAR,
    profile_id VARCHAR,
    room_id VARCHAR,
    action_type VARCHAR,
    reason VARCHAR,
    moderator_id VARCHAR,
    created_at TIMESTAMP,
    content VARCHAR,
    PRIMARY KEY (room_id, action_type, created_at)
) WITH CLUSTERING ORDER BY (action_type DESC, created_at DESC);

-- Message search index (future use)
CREATE TABLE IF NOT EXISTS messages.search (
    room_id VARCHAR,
    content_token VARCHAR,
    message_id VARCHAR,
    created_at TIMESTAMP,
    profile_id VARCHAR,
    content VARCHAR,
    PRIMARY KEY ((room_id, content_token), created_at, message_id)
) WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);