CREATE KEYSPACE IF NOT EXISTS messages WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
CREATE KEYSPACE IF NOT EXISTS notifications WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

-- Messages by room
CREATE TABLE IF NOT EXISTS messages.messages (
    room_id VARCHAR,
    created_at TIMESTAMP,
    profile_id VARCHAR,
    username VARCHAR,
    recipient_profile_id VARCHAR,
    recipient_username VARCHAR,
    content TEXT,
    resource_id VARCHAR,
    resource_type VARCHAR,
    thread_id VARCHAR,
    edited_at VARCHAR,
    deleted_at VARCHAR,
    pinned BOOLEAN,
    read_at TIMESTAMP,
    metadata MAP<VARCHAR, VARCHAR>,
    PRIMARY KEY (room_id, created_at, profile_id)
) WITH CLUSTERING ORDER BY (created_at DESC, profile_id ASC);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications.notifications (
    profile_id VARCHAR,
    created_at TIMESTAMP,
    type VARCHAR,
    related_id VARCHAR,
    content VARCHAR,
    metadata MAP<VARCHAR, VARCHAR>,
    PRIMARY KEY (profile_id, created_at, type)
) WITH CLUSTERING ORDER BY (created_at DESC, type ASC);

-- Read Notifications
CREATE TABLE IF NOT EXISTS notifications.read_notifications (
    profile_id VARCHAR,
    created_at TIMESTAMP,
    type VARCHAR,
    read_at TIMESTAMP,
    related_id VARCHAR,
    content VARCHAR,
    metadata MAP<VARCHAR, VARCHAR>,
    PRIMARY KEY (profile_id, created_at, type)
) WITH CLUSTERING ORDER BY (created_at DESC, type ASC);
