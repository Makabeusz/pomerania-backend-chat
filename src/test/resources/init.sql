-- Create lifestyle table (no foreign keys yet)
CREATE TABLE lifestyle (
    id VARCHAR(255) PRIMARY KEY,
    relationship_status VARCHAR(50),
    hobbies JSONB,
    purposes JSONB,
    have_children VARCHAR(50),
    want_children VARCHAR(50),
    smoking VARCHAR(50),
    drinking VARCHAR(50),
    diet VARCHAR(50),
    sport VARCHAR(50),
    profile_id VARCHAR(255) UNIQUE
);

-- Create profiles table (no foreign keys yet)
CREATE TABLE profiles (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50),
    image_192 TEXT,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    description TEXT,
    gender VARCHAR(50),
    height VARCHAR(50),
    weight VARCHAR(50),
    birthdate DATE,
    firstname VARCHAR(100),
    address VARCHAR(255),
    body VARCHAR(50),
    ethnicity VARCHAR(50),
    education VARCHAR(50),
    industry VARCHAR(100),
    religion VARCHAR(50),
    blocked_users JSONB,
    active VARCHAR(50),
    lifestyle_id VARCHAR(255) UNIQUE
);

-- Create personal_languages collection table
CREATE TABLE personal_languages (
    profile_id VARCHAR(255) NOT NULL,
    language VARCHAR(50) NOT NULL,
    PRIMARY KEY (profile_id, language)
);

-- Create settings_blocked_users collection table
CREATE TABLE settings_blocked_users (
    profile_id VARCHAR(255) NOT NULL,
    blocked_user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (profile_id, blocked_user_id)
);

-- Create lifestyle_interested_in collection table
CREATE TABLE lifestyle_interested_in (
    lifestyle_id VARCHAR(255) NOT NULL,
    value VARCHAR(50) NOT NULL,
    min INTEGER,
    max INTEGER,
    PRIMARY KEY (lifestyle_id, value)
);

-- Create galleries table
CREATE TABLE galleries (
    id VARCHAR(255) PRIMARY KEY,
    profile_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    gallery_order INTEGER,
    visibility VARCHAR(10),
    gallery_name VARCHAR(255)
);

-- Create resources table
CREATE TABLE resources (
    id VARCHAR(255) PRIMARY KEY,
    gallery_id VARCHAR(255),
    published_at TIMESTAMP NOT NULL,
    url VARCHAR(2048),
    description TEXT,
    type VARCHAR(20) NOT NULL
);

-- Create users auth table
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Create friends table
CREATE TABLE friends (
    profile_id VARCHAR(255) NOT NULL,
    friend_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id, friend_id)
);

-- Create followers table
CREATE TABLE followers (
    profile_id VARCHAR(255) NOT NULL,
    follower_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create posts table
CREATE TABLE posts (
    id VARCHAR(255) PRIMARY KEY,
    profile_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    resource_id VARCHAR(255), -- Optional photo
    visibility VARCHAR(20) NOT NULL, -- PUBLIC, FRIENDS, PRIVATE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_posts_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_posts_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE SET NULL
);

-- Create comments table
CREATE TABLE comments (
    id VARCHAR(255) PRIMARY KEY,
    post_id VARCHAR(255) NOT NULL,
    profile_id VARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL, -- Denormalized for performance
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_username FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

-- Add foreign key constraints
ALTER TABLE profiles
    ADD CONSTRAINT fk_profiles_lifestyle FOREIGN KEY (lifestyle_id) REFERENCES lifestyle(id) ON DELETE SET NULL;

ALTER TABLE profiles
    ADD CONSTRAINT fk_profiles_users FOREIGN KEY (username) REFERENCES users(username) ON DELETE SET NULL;

ALTER TABLE profiles
    ADD CONSTRAINT fk_profiles_users_id FOREIGN KEY (id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE lifestyle
    ADD CONSTRAINT fk_lifestyle_profiles FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE SET NULL;

ALTER TABLE personal_languages
    ADD CONSTRAINT fk_personal_languages_profiles FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;

ALTER TABLE settings_blocked_users
    ADD CONSTRAINT fk_settings_blocked_users_profiles FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;

ALTER TABLE lifestyle_interested_in
    ADD CONSTRAINT fk_lifestyle_interested_in_lifestyle FOREIGN KEY (lifestyle_id) REFERENCES lifestyle(id) ON DELETE CASCADE;

ALTER TABLE galleries
    ADD CONSTRAINT fk_galleries_profiles FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;

ALTER TABLE resources
    ADD CONSTRAINT fk_resources_galleries FOREIGN KEY (gallery_id) REFERENCES galleries(id) ON DELETE CASCADE;

ALTER TABLE friends
    ADD CONSTRAINT fk_friends_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;

ALTER TABLE friends
    ADD CONSTRAINT fk_friends_friend FOREIGN KEY (friend_id) REFERENCES profiles(id) ON DELETE CASCADE;

ALTER TABLE friends
    ADD CONSTRAINT check_not_self CHECK (profile_id != friend_id);

ALTER TABLE followers
    ADD CONSTRAINT fk_followers_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;

ALTER TABLE followers
    ADD CONSTRAINT fk_followers_follower FOREIGN KEY (follower_id) REFERENCES profiles(id) ON DELETE CASCADE;

ALTER TABLE followers
    ADD CONSTRAINT check_not_self CHECK (profile_id != follower_id);

-- Create indexes for performance
CREATE INDEX idx_profiles_gender ON profiles(gender);
CREATE INDEX idx_profiles_location ON profiles(address);
CREATE INDEX idx_profiles_relationship_status ON lifestyle(relationship_status);
CREATE INDEX idx_personal_languages_profile_id ON personal_languages(profile_id);
CREATE INDEX idx_settings_blocked_users_profile_id ON settings_blocked_users(profile_id);
CREATE INDEX idx_lifestyle_interested_in_lifestyle_id ON lifestyle_interested_in(lifestyle_id);
CREATE INDEX idx_lifestyle_interested_in_value ON lifestyle_interested_in(value);
CREATE INDEX idx_galleries_profile_id ON galleries(profile_id);
CREATE INDEX idx_resources_gallery_id ON resources(gallery_id);
CREATE INDEX idx_lifestyle_hobbies ON lifestyle USING GIN (hobbies);
CREATE INDEX idx_lifestyle_purposes ON lifestyle USING GIN (purposes);
CREATE INDEX idx_followers_profile_id ON followers(profile_id);
CREATE INDEX idx_followers_follower_id ON followers(follower_id);
CREATE INDEX idx_friends_profile_id ON friends(profile_id);
CREATE INDEX idx_friends_friend_id ON friends(friend_id);
CREATE INDEX idx_posts_profile_id ON posts(profile_id);
CREATE INDEX idx_posts_created_at ON posts(created_at);
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_profile_id ON comments(profile_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);