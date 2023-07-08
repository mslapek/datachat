-- postgresql sample db

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS posts (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT INTO users (name, country) VALUES
    ('John Doe', 'USA'),
    ('Jane Doe', 'UK'),
    ('Bob Smith', 'USA'),
    ('Mary Jane', 'USA'),
    ('Richard Roe', 'USA');

INSERT INTO posts (user_id, title, body) VALUES
    (1, 'Hello World', 'This is my first post.'),
    (1, 'Second Post', 'This is my second post.'),
    (2, 'Goodbye', 'So long and thanks for all the fish.'),
    (3, 'Test Post', 'This is a test post.'),
    (4, 'Another Post', 'Yet another post.'),
    (5, 'The Last Post', 'This is the last post.');
