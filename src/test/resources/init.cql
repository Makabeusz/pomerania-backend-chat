CREATE KEYSPACE IF NOT EXISTS messages WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE messages.messages (
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

CREATE TABLE messages.conversations (
    room_id VARCHAR,
    user_id VARCHAR,
    last_message_created_at TIMESTAMP,
    PRIMARY KEY (room_id, user_id)
);

CREATE INDEX conversations_user_id_idx ON messages.conversations (user_id);

CREATE TABLE messages.reactions (
    message_id VARCHAR,
    content VARCHAR,
    profile_id VARCHAR,
    created_at TIMESTAMP,
    PRIMARY KEY (message_id),
);

CREATE TABLE messages.read_reactions (
    id VARCHAR,
    profile_id VARCHAR,
    username VARCHAR,
    read_at TIMESTAMP,
    content VARCHAR,
    PRIMARY KEY (id, profile_id)
);

CREATE TABLE messages.group_members (
    group_id VARCHAR,
    profile_id VARCHAR,
    joined_at TIMESTAMP,
    role VARCHAR,
    content VARCHAR,
    PRIMARY KEY (group_id, profile_id)
);

CREATE TABLE messages.typing_indicators (
    room_id VARCHAR,
    profile_id VARCHAR,
    username VARCHAR,
    last_typed_at TIMESTAMP,
    content VARCHAR,
    PRIMARY KEY (room_id, profile_id)
);

CREATE TABLE messages.actions (
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

CREATE TABLE messages.search (
    room_id VARCHAR,
    content_token VARCHAR,
    message_id VARCHAR,
    created_at TIMESTAMP,
    profile_id VARCHAR,
    content VARCHAR,
    PRIMARY KEY ((room_id, content_token), created_at, message_id)
) WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);