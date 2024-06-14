DROP TABLE IF EXISTS friend_request_from_user;
DROP TABLE IF EXISTS friend_request_for_user;
DROP TABLE IF EXISTS friend;
DROP TABLE IF EXISTS user_chat;
DROP TABLE IF EXISTS chat;
DROP TABLE IF EXISTS "user";

CREATE TABLE "user" (
    user_id int GENERATED ALWAYS AS IDENTITY,
    username text NOT NULL,
    password text NOT NULL,
    PRIMARY KEY(user_id)
);

CREATE TABLE chat (
    chat_id int GENERATED ALWAYS AS IDENTITY,
    name text NOT NULL,
    type varchar NOT NULL,
    messages text[],
    PRIMARY KEY(chat_id),
    CHECK(type IN ('G', 'C'))
);

CREATE TABLE user_chat (
    user_id int REFERENCES "user"(user_id),
    chat_id int REFERENCES chat(chat_id)
);

CREATE TABLE friend (
    user1_id int REFERENCES "user"(user_id),
    user2_id int REFERENCES "user"(user_id)
);

CREATE TABLE friend_request_for_user (
    user_id int REFERENCES "user"(user_id),
    from_user_id int REFERENCES "user"(user_id)
);

CREATE TABLE friend_request_from_user (
    user_id int REFERENCES "user"(user_id),
    to_user_id int REFERENCES "user"(user_id)
);