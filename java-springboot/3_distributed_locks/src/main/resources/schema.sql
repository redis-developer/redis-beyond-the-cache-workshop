CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    quantity INTEGER NOT NULL
);

INSERT INTO inventory (id, name, quantity)
VALUES (1, 'limited-run-hoodie', 5)
ON CONFLICT (id) DO NOTHING;
