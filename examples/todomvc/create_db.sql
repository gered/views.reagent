-- For PostgreSQL
-- run with psql. e.g. 'psql < create_db.sql'

CREATE ROLE todomvc LOGIN PASSWORD 's3cr3t';
CREATE DATABASE todomvc OWNER todomvc;

-- assumes you're piping this script into psql ...
\c todomvc;

CREATE TABLE todos
(
  id SERIAL PRIMARY KEY NOT NULL,
  title TEXT NOT NULL,
  done BOOLEAN DEFAULT FALSE NOT NULL
);
ALTER TABLE todos OWNER TO todomvc;

INSERT INTO todos (title, done) VALUES ('Rename Cloact to Reagent', TRUE);
INSERT INTO todos (title, done) VALUES ('Add undo demo', TRUE);
INSERT INTO todos (title, done) VALUES ('Make all rendering async', TRUE);
INSERT INTO todos (title, done) VALUES ('Allow any arguments to component functions', TRUE);
