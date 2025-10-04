-- Create lifestyle table (no foreign keys yet)
CREATE TABLE IF NOT EXISTS lifestyle (
    id VARCHAR(255) PRIMARY KEY,
    profile_id VARCHAR(255),
    pair_order INTEGER NOT NULL,
    relationship_status VARCHAR(50),
    hobbies JSONB,
    purposes JSONB,
    have_children VARCHAR(50),
    want_children VARCHAR(50),
    smoking VARCHAR(50),
    drinking VARCHAR(50),
    diet VARCHAR(50),
    sport VARCHAR(50)
);

-- Create profiles table (no foreign keys yet)
CREATE TABLE IF NOT EXISTS profiles (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    image_192 TEXT,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    is_pair BOOLEAN,
    description TEXT,
    city_id VARCHAR(255),
    blocked_users JSONB,
    active VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS personal (
    id VARCHAR(255) NOT NULL UNIQUE,
    profile_id VARCHAR(255) NOT NULL,
    pair_order INTEGER NOT NULL,
    firstname VARCHAR(100),
    gender VARCHAR(50), -- add not null
    gender_changed_count INTEGER NOT NULL DEFAULT 0,
    sub_gender VARCHAR(50),
    height INTEGER,
    weight INTEGER,
    birthdate DATE,
    body VARCHAR(50),
    ethnicity VARCHAR(50),
    education VARCHAR(50),
    industry VARCHAR(100),
    religion VARCHAR(50),
    languages JSONB,
    PRIMARY KEY(id, profile_id)
);

-- Create settings_blocked_users collection table
CREATE TABLE IF NOT EXISTS settings_blocked_users (
    profile_id VARCHAR(255) NOT NULL,
    blocked_user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (profile_id, blocked_user_id)
);

-- Create profile_interested_in collection table
CREATE TABLE IF NOT EXISTS profile_interested_in (
    profile_id VARCHAR(255) NOT NULL,
    value VARCHAR(50) NOT NULL,
    min INTEGER,
    max INTEGER,
    PRIMARY KEY (profile_id, value)
);

-- Create galleries table
CREATE TABLE IF NOT EXISTS galleries (
    id VARCHAR(255) PRIMARY KEY,
    profile_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    gallery_order INTEGER,
    visibility VARCHAR(10),
    gallery_name VARCHAR(255)
);

-- Create resources table
CREATE TABLE IF NOT EXISTS resources (
    id VARCHAR(255) PRIMARY KEY,
    gallery_id VARCHAR(255),
    published_at TIMESTAMP NOT NULL,
    url VARCHAR(2048),
    description TEXT,
    type VARCHAR(20) NOT NULL
);

-- Create users auth table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Create friends table
CREATE TABLE IF NOT EXISTS friends (
    profile_id VARCHAR(255) NOT NULL,
    friend_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id, friend_id)
);

-- Create followers table
CREATE TABLE IF NOT EXISTS followers (
    profile_id VARCHAR(255) NOT NULL,
    follower_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create posts table
CREATE TABLE IF NOT EXISTS posts (
    id VARCHAR(255) PRIMARY KEY,
    profile_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    resource_id VARCHAR(255), -- Optional photo
    visibility VARCHAR(20) NOT NULL, -- PUBLIC, FRIENDS, PRIVATE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create comments table
CREATE TABLE IF NOT EXISTS comments (
    id VARCHAR(255) PRIMARY KEY,
    related_id VARCHAR(255) NOT NULL,
    related_type VARCHAR(50) NOT NULL,
    related_profile_id VARCHAR(255) NOT NULL,
    profile_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create likes table
CREATE TABLE IF NOT EXISTS likes (
    profile_id VARCHAR(255) NOT NULL,
    related_id VARCHAR(255) NOT NULL,
    related_type VARCHAR(50) NOT NULL, -- e.g., 'POST', 'PHOTO', 'PROFILE'
    related_profile_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- currently only 'HEART'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id, related_id)
);

-- messages backup START --
CREATE TABLE IF NOT EXISTS conversations (
    user_id VARCHAR(255),
    room_id VARCHAR(255),
    image_192 VARCHAR(36),
    last_message_at TIMESTAMP, -- indexed
    PRIMARY KEY (user_id, room_id)
);

CREATE TABLE IF NOT EXISTS message_notifications (
    profile_id VARCHAR,
    created_at TIMESTAMP,
    sender_id VARCHAR,
    sender_username VARCHAR,
    content VARCHAR,
    PRIMARY KEY (profile_id, created_at, sender_id)
);
-- messages backup END --

CREATE TABLE IF NOT EXISTS osmcities (
        id VARCHAR(255) PRIMARY KEY,
        geom GEOMETRY,
        tags JSONB,
        city_name TEXT,
        province TEXT,
        municipality TEXT,
        country TEXT,
        pop INTEGER
);

-- Add foreign key constraints
ALTER TABLE profiles ADD CONSTRAINT fk_profiles_users FOREIGN KEY (username) REFERENCES users(username) ON DELETE SET NULL;
ALTER TABLE profiles ADD CONSTRAINT fk_profiles_users_id FOREIGN KEY (id) REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE lifestyle ADD CONSTRAINT fk_lifestyle_profiles FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE SET NULL;
ALTER TABLE settings_blocked_users ADD CONSTRAINT fk_settings_blocked_users_profiles FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE profile_interested_in ADD CONSTRAINT fk_profile_interested_in_lifestyle FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE galleries ADD CONSTRAINT fk_galleries_profiles FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE resources ADD CONSTRAINT fk_resources_galleries FOREIGN KEY (gallery_id) REFERENCES galleries(id) ON DELETE CASCADE;
ALTER TABLE friends ADD CONSTRAINT fk_friends_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE friends ADD CONSTRAINT fk_friends_friend FOREIGN KEY (friend_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE friends ADD CONSTRAINT check_not_self CHECK (profile_id != friend_id);
ALTER TABLE followers ADD CONSTRAINT fk_followers_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE followers ADD CONSTRAINT fk_followers_follower FOREIGN KEY (follower_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE followers ADD CONSTRAINT check_not_self CHECK (profile_id != follower_id);
ALTER TABLE profiles ADD CONSTRAINT fk_profiles_osmcity FOREIGN KEY (city_id) REFERENCES osmcities(id) ON DELETE SET NULL;
ALTER TABLE personal ADD CONSTRAINT fk_personal_profile_id FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE SET NULL;
ALTER TABLE likes ADD CONSTRAINT fk_likes_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE likes ADD CONSTRAINT check_unique_like UNIQUE (profile_id, related_id);
ALTER TABLE posts ADD CONSTRAINT fk_posts_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE posts ADD CONSTRAINT fk_posts_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE SET NULL;
ALTER TABLE comments ADD CONSTRAINT fk_comments_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_profiles_city_id ON profiles(city_id);
CREATE INDEX IF NOT EXISTS idx_profiles_relationship_status ON lifestyle(relationship_status);
CREATE INDEX IF NOT EXISTS idx_personal_gender ON personal(gender);
CREATE INDEX IF NOT EXISTS idx_settings_blocked_users_profile_id ON settings_blocked_users(profile_id);
CREATE INDEX IF NOT EXISTS idx_galleries_profile_id ON galleries(profile_id);
CREATE INDEX IF NOT EXISTS idx_resources_gallery_id ON resources(gallery_id);
CREATE INDEX IF NOT EXISTS idx_lifestyle_hobbies ON lifestyle USING GIN (hobbies);
CREATE INDEX IF NOT EXISTS idx_lifestyle_purposes ON lifestyle USING GIN (purposes);
CREATE INDEX IF NOT EXISTS idx_followers_profile_id ON followers(profile_id);
CREATE INDEX IF NOT EXISTS idx_followers_follower_id ON followers(follower_id);
CREATE INDEX IF NOT EXISTS idx_friends_profile_id ON friends(profile_id);
CREATE INDEX IF NOT EXISTS idx_friends_friend_id ON friends(friend_id);
CREATE INDEX IF NOT EXISTS idx_posts_profile_id ON posts(profile_id);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at);
CREATE INDEX IF NOT EXISTS idx_comments_related ON comments(related_id);
CREATE INDEX IF NOT EXISTS idx_comments_created_at ON comments(created_at);
CREATE INDEX IF NOT EXISTS idx_comments_profile_id ON comments(profile_id);
CREATE INDEX IF NOT EXISTS idx_comments_related_profile_id ON comments(related_profile_id);
CREATE INDEX IF NOT EXISTS idx_conversations_last_message_at ON conversations(last_message_at);
CREATE INDEX IF NOT EXISTS idx_message_notifications_created_at ON message_notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_likes_related_id ON likes(related_id);
CREATE INDEX IF NOT EXISTS idx_likes_type ON likes(type);
CREATE INDEX IF NOT EXISTS idx_likes_related_profile_id ON likes(related_profile_id);
CREATE INDEX IF NOT EXISTS idx_osmcities_geom ON osmcities USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_osmcities_city_name ON osmcities (city_name);
CREATE INDEX IF NOT EXISTS idx_osmcities_province ON osmcities (province);
CREATE INDEX IF NOT EXISTS idx_osmcities_municipality ON osmcities (municipality);
CREATE INDEX IF NOT EXISTS idx_osmcities_country ON osmcities (country);
CREATE INDEX IF NOT EXISTS idx_osmcities_pop ON osmcities (pop);
