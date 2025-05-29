CREATE KEYSPACE IF NOT EXISTS messages WITH replication = {'class': 'SimpleStrategy', 'limit': 1};

-- Messages by room (used in this example)
CREATE TABLE messages.messages_by_room (
    room_id TEXT,
    created_at TIMESTAMP,
    message_id TEXT,
    profile_id VARCHAR,
    username TEXT,
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

-- Messages by user (future use)
CREATE TABLE messages.messages_by_room_id (
    profile_id TEXT,
    created_at TIMESTAMP,
    message_id TEXT,
    room_id VARCHAR,
    username TEXT,
    content TEXT,
    message_type TEXT,
    resource_id TEXT,
    thread_id TEXT,
    edited_at VARCHAR,
    deleted_at TEXT,
    pinned BOOLEAN,
    metadata MAP<TEXT, TEXT>,
    PRIMARY KEY (profile_id, created_at, message_id)
) WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);

-- Reactions (future use)
CREATE TABLE messages.reactions (
    message_id TEXT,
    content TEXT,
    profile_id INTEGER,
    created_at TIMESTAMP,
    PRIMARY KEY (message_id),
);

-- Read receipts (future use)
CREATE TABLE messages.read_reactions (
    id TEXT,
    profile_id INTEGER,
    username TEXT,
    read_at TIMESTAMP,
    content TEXT,
    PRIMARY KEY (id, profile_id)
);

-- Group memberships (future use)
CREATE TABLE messages.group_members (
    group_id TEXT,
    profile_id TEXT,
    joined_at TIMESTAMP,
    role TEXT,
    content TEXT,
    PRIMARY KEY (group_id, profile_id)
);

-- Typing indicators (future use)
CREATE TABLE messages.typing_indicators (
    room_id TEXT,
    profile_id TEXT,
    username TEXT,
    last_typed_at TIMESTAMP,
    content TEXT,
    PRIMARY KEY (room_id, profile_id)
);

-- Moderation actions (future use)
CREATE TABLE messages.actions (
    action_id INTEGER,
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