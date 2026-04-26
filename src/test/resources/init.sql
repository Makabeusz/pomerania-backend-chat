-- Create lifestyle table (no foreign keys yet)
CREATE TABLE IF NOT EXISTS lifestyle (
    id UUID PRIMARY KEY,
    profile_id UUID,
    pair_order INTEGER NOT NULL,
    relationship_status VARCHAR(50),
    purposes JSONB,
    stimulants VARCHAR(30),
    height INTEGER,
    weight INTEGER,
    body VARCHAR(50),
    ethnicity VARCHAR(50),
    education VARCHAR(50),
    industry VARCHAR(100),
    languages JSONB
);

-- Create profiles table (no foreign keys yet)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    image_192 UUID,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    is_online BOOLEAN DEFAULT false,
    is_pair BOOLEAN,
    description TEXT,
    city_id UUID,
    validation_status VARCHAR DEFAULT 'RAW',
    sexual_preferences JSONB
);

CREATE TABLE IF NOT EXISTS sexualities (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    pair_order INTEGER NOT NULL,
    orientation VARCHAR,
    sex_readiness VARCHAR,
    penis_size VARCHAR,
    sexual_role VARCHAR,
    sex_climate JSONB,
    experience VARCHAR,
    recording VARCHAR,
    group_sex JSONB
);

-- New user_preferences table for non-core settings
CREATE TABLE IF NOT EXISTS profile_preferences (
    profile_id UUID PRIMARY KEY,
    language VARCHAR(2) DEFAULT 'pl',
    region VARCHAR(2) DEFAULT 'PL',
    notifications jsonb DEFAULT '{}'::jsonb
 );

CREATE TABLE IF NOT EXISTS personal (
    id UUID NOT NULL UNIQUE,
    profile_id UUID NOT NULL,
    pair_order INTEGER NOT NULL,
    firstname VARCHAR(100),
    gender VARCHAR(50) NOT NULL,
    gender_changed_count INTEGER NOT NULL DEFAULT 0,
    sub_gender VARCHAR(50),
    birthdate DATE,
    PRIMARY KEY(id, profile_id)
);

-- Create settings_blocked_users collection table
CREATE TABLE IF NOT EXISTS settings_blocked_users (
    profile_id UUID NOT NULL,
    blocked_user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (profile_id, blocked_user_id)
);

-- Create profile_interested_in collection table
CREATE TABLE IF NOT EXISTS profile_interested_in (
    profile_id UUID NOT NULL,
    value VARCHAR(50) NOT NULL,
    min INTEGER,
    max INTEGER,
    PRIMARY KEY (profile_id, value)
);

-- Create galleries table
CREATE TABLE IF NOT EXISTS galleries (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    gallery_order INTEGER,
    visibility VARCHAR(10) NOT NULL,
    gallery_name VARCHAR(255)
);

-- Create resources table
CREATE TABLE IF NOT EXISTS resources (
    id UUID PRIMARY KEY,
    gallery_id UUID,
    published_at TIMESTAMP NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    flag VARCHAR(10) NOT NULL DEFAULT 'RAW',
    thumbnail_id UUID
);

CREATE TABLE IF NOT EXISTS thumbnails (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_thumbnails (
    id UUID PRIMARY KEY,
    resource_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_resources (
    id UUID PRIMARY KEY,
    room_id VARCHAR(73),
    profile_id UUID,
    published_at TIMESTAMP NOT NULL,
    ttl TIMESTAMP,
    views INTEGER,
    type VARCHAR(20) NOT NULL,
    thumbnail_id UUID
);

-- Create users auth table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    _2fa VARCHAR(15) NOT NULL,
    enabled BOOLEAN
);

CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID REFERENCES users(id),
    role_id INT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

-- Token for verification purposes i.e. e-mail confirmation
CREATE TABLE IF NOT EXISTS verification_token (
    token TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    create_date TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY(token, type, user_id)
);

CREATE TABLE IF NOT EXISTS banned_users (
    user_id UUID REFERENCES users(id) PRIMARY KEY,
    role_id INT REFERENCES roles(id),
    expiry_date TIMESTAMP NOT NULL,
    create_date TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create friends table
CREATE TABLE IF NOT EXISTS friends (
    profile_id UUID NOT NULL,
    friend_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id, friend_id)
);

-- Create followers table
CREATE TABLE IF NOT EXISTS followers (
    profile_id UUID NOT NULL,
    follower_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create posts table
CREATE TABLE IF NOT EXISTS posts (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    content TEXT NOT NULL,
    resource_id UUID, -- Optional photo/video id
    visibility VARCHAR(20) NOT NULL, -- PUBLIC, PRIVATE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create comments table
CREATE TABLE IF NOT EXISTS comments (
    id UUID PRIMARY KEY,
    related_id UUID NOT NULL,
    related_type VARCHAR(50) NOT NULL,
    related_profile_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    pair_author_personal_id UUID
);

-- Create likes table
CREATE TABLE IF NOT EXISTS likes (
    profile_id UUID NOT NULL,
    related_id UUID NOT NULL,
    related_type VARCHAR(50) NOT NULL, -- e.g., 'POST', 'PHOTO', 'PROFILE'
    related_profile_id UUID NOT NULL,
    comment_type VARCHAR(64),
    comment_type_owner_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (profile_id, related_id)
);

CREATE TABLE IF NOT EXISTS conversations (
    user_id UUID,
    recipient_id UUID,
    flag VARCHAR DEFAULT 'NORMAL' NOT NULL,
    last_message_at TIMESTAMP,
    content VARCHAR,
    content_type VARCHAR,
    unread_count INTEGER,
    is_last_message_from_user BOOLEAN,
    PRIMARY KEY (user_id, recipient_id)
);

CREATE TABLE IF NOT EXISTS osmcities (
    id UUID PRIMARY KEY,
    geom GEOMETRY,
    tags JSONB,
    city_name TEXT,
    province TEXT,
    municipality TEXT,
    country TEXT,
    pop INTEGER
);


-- TODO: Add to account removal, delete the photo from buckets
CREATE TABLE IF NOT EXISTS validation_photos (
    profile_id UUID PRIMARY KEY,
    photo_id UUID,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    status VARCHAR,
    reason VARCHAR
);

CREATE TABLE IF NOT EXISTS profile_views (
    profile_id UUID,
    day DATE,
    viewer_id UUID,
    count INTEGER,
    available BOOLEAN DEFAULT false,
    first_timestamp TIMESTAMP NOT NULL,
    last_timestamp TIMESTAMP  NOT NULL,
    PRIMARY KEY (profile_id, day, viewer_id)
);

-- Add foreign key constraints with ON DELETE CASCADE
ALTER TABLE profiles ADD CONSTRAINT fk_profiles_users FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE;
ALTER TABLE profiles ADD CONSTRAINT fk_profiles_users_id FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE profiles ADD CONSTRAINT fk_profiles_osmcity FOREIGN KEY (city_id) REFERENCES osmcities(id) ON DELETE SET NULL;
ALTER TABLE personal ADD CONSTRAINT fk_personal_profile_id FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE lifestyle ADD CONSTRAINT fk_lifestyle_profiles FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
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
ALTER TABLE likes ADD CONSTRAINT fk_likes_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE likes ADD CONSTRAINT check_unique_like UNIQUE (profile_id, related_id);
ALTER TABLE posts ADD CONSTRAINT fk_posts_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE posts ADD CONSTRAINT fk_posts_resource FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE SET NULL;
ALTER TABLE comments ADD CONSTRAINT fk_comments_profile FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE comments ADD CONSTRAINT fk_comments_personal FOREIGN KEY (pair_author_personal_id, profile_id) REFERENCES personal(id, profile_id) ON DELETE CASCADE;
ALTER TABLE verification_token ADD CONSTRAINT fk_verification_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
-- TODO: those are disabled for tests, implement some helper class to fulfill this condition
--ALTER TABLE conversations ADD CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE profile_preferences ADD CONSTRAINT fk_profile_preferences_user FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
ALTER TABLE validation_photos ADD CONSTRAINT fk_validation_photos_user FOREIGN KEY (profile_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE sexualities ADD CONSTRAINT fk_sexualities_user FOREIGN KEY (profile_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE thumbnails ADD CONSTRAINT fk_thumbnails_resources FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE;
ALTER TABLE thumbnails ADD CONSTRAINT uq_thumbnails_resource_id UNIQUE (resource_id);
ALTER TABLE chat_thumbnails ADD CONSTRAINT fk_chat_thumbnails_resources FOREIGN KEY (resource_id) REFERENCES chat_resources(id) ON DELETE CASCADE;
ALTER TABLE chat_thumbnails ADD CONSTRAINT uq_chat_thumbnails_resource_id UNIQUE (resource_id);

--ALTER TABLE user_tokens ADD CONSTRAINT fk_user_tokens_users FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_profiles_city_id ON profiles(city_id);
CREATE INDEX IF NOT EXISTS idx_profiles_relationship_status ON lifestyle(relationship_status);
CREATE INDEX IF NOT EXISTS idx_personal_gender ON personal(gender);
CREATE INDEX IF NOT EXISTS idx_galleries_profile_id ON galleries(profile_id);
CREATE INDEX IF NOT EXISTS idx_galleries_gallery_name ON galleries(gallery_name);
CREATE INDEX IF NOT EXISTS idx_resources_gallery_id ON resources(gallery_id);
CREATE INDEX IF NOT EXISTS idx_resources_admin_flag ON resources(flag);
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
CREATE INDEX IF NOT EXISTS idx_conversations_flag ON conversations(flag);
CREATE INDEX IF NOT EXISTS idx_likes_created_at ON likes(created_at);
CREATE INDEX IF NOT EXISTS idx_likes_related_profile_id ON likes(related_profile_id);
CREATE INDEX IF NOT EXISTS idx_osmcities_geom ON osmcities USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_osmcities_city_name ON osmcities (city_name);
CREATE INDEX IF NOT EXISTS idx_osmcities_province ON osmcities (province);
CREATE INDEX IF NOT EXISTS idx_osmcities_municipality ON osmcities (municipality);
CREATE INDEX IF NOT EXISTS idx_osmcities_country ON osmcities (country);
CREATE INDEX IF NOT EXISTS idx_osmcities_pop ON osmcities (pop);
CREATE INDEX IF NOT EXISTS idx_profile_preferences_profile_id ON profile_preferences(profile_id);
CREATE INDEX IF NOT EXISTS idx_profile_views_last_timestamp ON profile_views(last_timestamp);

--handle roles
INSERT INTO roles (id, name) VALUES (0, 'ADMIN');
INSERT INTO roles (id, name) VALUES (1, 'USER');
INSERT INTO roles (id, name) VALUES (2, 'DEACTIVATED');
INSERT INTO roles (id, name) VALUES (3, 'SOFT_BAN');
INSERT INTO roles (id, name) VALUES (4, 'HARD_BAN');

-----------------–-----------------------–-----------------------–-------------
-----------------–------ Security blocked user context -----------------–------
-----------------–-----------------------–-----------------------–-------------

CREATE UNLOGGED TABLE IF NOT EXISTS app_session_context (
    id SERIAL PRIMARY KEY,
    current_user_id UUID NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure only one row exists
DELETE FROM app_session_context;
INSERT INTO app_session_context (current_user_id) VALUES ('00000000-0000-0000-0000-000000000000');

-- 2. Secure function to set current user (only your app can call it)
CREATE OR REPLACE FUNCTION public.set_current_user(user_id UUID)
RETURNS void AS $$
BEGIN
    UPDATE app_session_context
    SET current_user_id = user_id, updated_at = NOW()
    WHERE id = 1;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- TODO: Grant only your app role permission
--REVOKE ALL ON FUNCTION set_current_user(UUID) FROM PUBLIC;
--GRANT EXECUTE ON FUNCTION set_current_user(UUID) TO your_app_role;


CREATE OR REPLACE VIEW v_profiles AS
SELECT p.*
FROM profiles p
JOIN users u ON p.id = u.id
WHERE NOT EXISTS (
    SELECT 1 FROM settings_blocked_users bu
    WHERE bu.profile_id = (SELECT current_user_id FROM app_session_context)
      AND bu.blocked_user_id = p.id
)
AND NOT EXISTS (
    SELECT 1 FROM settings_blocked_users bu
    WHERE bu.profile_id = p.id
      AND bu.blocked_user_id = (SELECT current_user_id FROM app_session_context)
)
AND u.enabled = true;

CREATE OR REPLACE VIEW v_comments AS
SELECT c.*
FROM comments c
WHERE c.profile_id IN (SELECT id FROM v_profiles);

CREATE OR REPLACE VIEW v_followers AS
SELECT f.*
FROM followers f
WHERE f.profile_id IN (SELECT id FROM v_profiles)
AND f.follower_id IN (SELECT id FROM v_profiles);

CREATE OR REPLACE VIEW v_friends AS
SELECT f.*
FROM friends f
WHERE f.profile_id IN (SELECT id FROM v_profiles)
AND f.friend_id IN (SELECT id FROM v_profiles);

CREATE OR REPLACE VIEW v_likes AS
SELECT l.*
FROM likes l
WHERE l.profile_id IN (SELECT id FROM v_profiles)
AND l.related_profile_id IN (SELECT id FROM v_profiles);

CREATE OR REPLACE VIEW v_profile_views AS
SELECT v.*
FROM profile_views v
WHERE v.viewer_id IN (SELECT id FROM v_profiles);

-- Special view for posts, includes a distance selector, like and comment counts,
-- latest comment, only USER role and visibility check
-- TODO: I can use current_user_id and will not be required to manually provide the current userId for each query...
CREATE OR REPLACE VIEW v_posts_core AS
SELECT
    p.*,
    pr.image_192 AS image192,
    pr.username,
    r.type AS resource_type,
    pr.is_pair,
    viewer.id AS viewer_profile_id,
    pr.city_id AS poster_city_id,
    viewer.city_id AS viewer_city_id,
    COALESCE(l.like_count, 0) AS like_count,
    COALESCE(c.comment_count, 0) AS comment_count,
    COALESCE(my_like.is_liked, false) AS is_liked,
    latest_comment.id AS latest_comment_id,
    latest_comment.content AS latest_comment_content,
    latest_comment.created_at AS latest_comment_created_at,
    latest_comment.profile_id AS latest_comment_profile_id,
    commenter.username AS latest_comment_username,
    commenter.image_192 AS latest_comment_image192,
    r.thumbnail_id
FROM posts p
JOIN v_profiles pr ON pr.id = p.profile_id
LEFT JOIN resources r ON r.id = p.resource_id
CROSS JOIN v_profiles viewer
JOIN user_roles ro ON pr.id = ro.user_id
LEFT JOIN (
    SELECT related_id, COUNT(*) AS like_count
    FROM likes
    WHERE related_type = 'POST'
    GROUP BY related_id
) l ON l.related_id = p.id
LEFT JOIN (
    SELECT related_id, COUNT(*) AS comment_count
    FROM comments
    WHERE related_type = 'POST'
    GROUP BY related_id
) c ON c.related_id = p.id
LEFT JOIN LATERAL (
    SELECT true AS is_liked
    FROM likes lk
    WHERE lk.related_type = 'POST'
      AND lk.related_id = p.id
      AND lk.profile_id = viewer.id
    LIMIT 1
) my_like ON true
LEFT JOIN LATERAL (
    SELECT
        c.id,
        c.profile_id,
        c.content,
        c.created_at
    FROM comments c
    WHERE c.related_type = 'POST'
      AND c.related_id = p.id
    ORDER BY c.created_at DESC
    LIMIT 1
) latest_comment ON true
LEFT JOIN v_profiles commenter ON commenter.id = latest_comment.profile_id
WHERE ro.role_id = 1;

CREATE OR REPLACE VIEW v_posts_base AS
SELECT * FROM v_posts_core
WHERE
    visibility = 'PUBLIC'
    OR EXISTS (
        SELECT 1
        FROM friends fr
        WHERE fr.profile_id = viewer_profile_id
          AND fr.friend_id = profile_id
          AND fr.status = 'FRIEND'
    );

CREATE OR REPLACE VIEW v_nearby_posts_base AS
SELECT
    base.*,
    ST_DistanceSphere(poster_city.geom, viewer_city.geom) AS distance_meters
FROM v_posts_base base
LEFT JOIN osmcities poster_city ON poster_city.id = base.poster_city_id
LEFT JOIN osmcities viewer_city ON viewer_city.id = base.viewer_city_id
WHERE base.poster_city_id IS NOT NULL
  AND base.viewer_city_id IS NOT NULL;

CREATE OR REPLACE VIEW v_personal_feed_posts_base AS
SELECT * FROM v_posts_core
WHERE
    profile_id = viewer_profile_id
    OR (
      EXISTS (
        SELECT 1
        FROM followers f
        WHERE f.profile_id = profile_id
          AND f.follower_id = viewer_profile_id
      )
      AND visibility = 'PUBLIC'
    )
    OR (
      EXISTS (
        SELECT 1
        FROM followers f
        WHERE f.profile_id = profile_id
          AND f.follower_id = viewer_profile_id
      )
      AND visibility = 'PRIVATE'
      AND EXISTS (
        SELECT 1
        FROM friends fr
        WHERE fr.profile_id = viewer_profile_id
          AND fr.friend_id = profile_id
          AND fr.status = 'FRIEND'
      )
    );

CREATE OR REPLACE VIEW v_post_enriched_with_visibility AS
SELECT
    core.*,
    (
        visibility = 'PUBLIC'
        OR (
            visibility = 'PRIVATE'
            AND (
                profile_id = viewer_profile_id
                OR EXISTS (
                    SELECT 1
                    FROM friends fr
                    WHERE fr.status = 'FRIEND'
                      AND (
                          (fr.profile_id = viewer_profile_id AND fr.friend_id = profile_id)
                          OR (fr.profile_id = profile_id AND fr.friend_id = viewer_profile_id)
                      )
                )
            )
        )
    ) AS is_visible
FROM v_posts_core core;


-- Fetch Resource view with its thumbnail, like and comment count, latest comment and a visibility check
CREATE OR REPLACE VIEW v_resources_core AS
SELECT
    r.id,
    r.published_at,
    r.description,
    r.type,
    r.thumbnail_id,
    p.id AS profile_id,
    p.username,
    p.image_192 AS image192,
    g.gallery_name,
    g.type AS gallery_type,
    g.visibility,
    po.id AS post_id,
    p.is_pair,
    viewer.id AS viewer_profile_id,
    COALESCE(l.like_count, 0) AS like_count,
    COALESCE(c.comment_count, 0) AS comment_count,
    COALESCE(my_like.is_liked, false) AS is_liked,
    latest_comment.content AS latest_comment_content,
    latest_comment.created_at AS latest_comment_created_at,
    latest_comment.profile_id AS latest_comment_profile_id,
    commenter.username AS latest_comment_username,
    commenter.image_192 AS latest_comment_image192
FROM resources r
JOIN galleries g ON g.id = r.gallery_id
JOIN v_profiles p ON g.profile_id = p.id
LEFT JOIN posts po ON po.resource_id = r.id
CROSS JOIN v_profiles viewer
JOIN user_roles ro ON p.id = ro.user_id
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS like_count
    FROM likes lk
    WHERE lk.related_id = COALESCE(po.id, r.id)
      AND lk.related_type = CASE WHEN po.id IS NOT NULL THEN 'POST' ELSE r.type END
) l ON true
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS comment_count
    FROM comments cm
    WHERE cm.related_id = COALESCE(po.id, r.id)
      AND cm.related_type = CASE WHEN po.id IS NOT NULL THEN 'POST' ELSE r.type END
) c ON true
LEFT JOIN LATERAL (
    SELECT true AS is_liked
    FROM likes lk
    WHERE lk.related_id = COALESCE(po.id, r.id)
      AND lk.related_type = CASE WHEN po.id IS NOT NULL THEN 'POST' ELSE r.type END
      AND lk.profile_id = viewer.id
    LIMIT 1
) my_like ON true
LEFT JOIN LATERAL (
    SELECT
        cm.id,
        cm.profile_id,
        cm.content,
        cm.created_at
    FROM comments cm
    WHERE cm.related_id = COALESCE(po.id, r.id)
      AND cm.related_type = CASE WHEN po.id IS NOT NULL THEN 'POST' ELSE r.type END
    ORDER BY cm.created_at DESC
    LIMIT 1
) latest_comment ON true
LEFT JOIN v_profiles commenter ON commenter.id = latest_comment.profile_id
WHERE ro.role_id = 1;





-- simple profile
CREATE OR REPLACE VIEW v_simple_profiles AS
SELECT
    p.id,
    p.username,
    p.image_192 AS image192,
    p.created_at,
    COALESCE(ARRAY_AGG(e.gender ORDER BY e.pair_order), ARRAY[]::VARCHAR[]) AS gender,
    COALESCE(ARRAY_AGG(EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.birthdate))::INTEGER ORDER BY e.pair_order), ARRAY[]::INTEGER[]) AS age,
    p.last_login_at,
    p.description,
    c.city_name,
    c.country,
    p.is_pair
FROM v_profiles p
JOIN personal e ON p.id = e.profile_id
JOIN user_roles r ON p.id = r.user_id
LEFT JOIN osmcities c ON c.id = p.city_id
WHERE r.role_id = 1
GROUP BY p.id, p.username, p.image_192, p.created_at, p.last_login_at, p.description, c.city_name, c.country, p.is_pair;

CREATE OR REPLACE VIEW v_galleries_core AS
SELECT
    g.*,
    viewer.id AS viewer_profile_id
FROM galleries g
CROSS JOIN (SELECT current_user_id AS id FROM app_session_context) viewer
WHERE g.profile_id IN (SELECT id FROM v_profiles)
  AND g.type = 'CUSTOM'
  AND EXISTS (
        SELECT 1 FROM user_roles ur
        WHERE ur.user_id = g.profile_id AND ur.role_id = 1
      )
  AND (
        g.profile_id = viewer.id
        OR g.visibility = 'PUBLIC'
        OR (
            g.visibility = 'PRIVATE'
            AND EXISTS (
                SELECT 1 FROM friends fr
                WHERE (fr.profile_id = viewer.id AND fr.friend_id = g.profile_id
                   OR fr.profile_id = g.profile_id AND fr.friend_id = viewer.id)
                  AND fr.status = 'FRIEND'
            )
        )
      )
  AND EXISTS (
          SELECT 1 FROM resources r
          WHERE r.gallery_id = g.id
      );


