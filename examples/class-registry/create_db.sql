-- For PostgreSQL
-- run with psql. e.g. 'psql < create_db.sql'

CREATE ROLE class_registry LOGIN PASSWORD 's3cr3t';
CREATE DATABASE class_registry OWNER class_registry;

-- assumes you're piping this script into psql ...
\c class_registry;

CREATE TABLE classes
(
  class_id SERIAL PRIMARY KEY NOT NULL,
  code     TEXT               NOT NULL,
  name     TEXT               NOT NULL
);
ALTER TABLE classes OWNER TO class_registry;

CREATE TABLE people
(
  people_id   SERIAL PRIMARY KEY NOT NULL,
  type        TEXT               NOT NULL,
  first_name  TEXT               NOT NULL,
  middle_name TEXT,
  last_name   TEXT               NOT NULL,
  email       TEXT               NOT NULL
);
ALTER TABLE people OWNER TO class_registry;

CREATE TABLE registry
(
    registry_id SERIAL PRIMARY KEY                                      NOT NULL,
    class_id    INTEGER REFERENCES classes (class_id) ON DELETE CASCADE NOT NULL,
    people_id   INTEGER REFERENCES people (people_id) ON DELETE CASCADE NOT NULL
);
ALTER TABLE registry OWNER TO class_registry;

INSERT INTO people (type, first_name, middle_name, last_name, email) VALUES ('student', 'Ben', NULL, 'Bitdiddle', 'benb@mit.edu');
INSERT INTO people (type, first_name, middle_name, last_name, email) VALUES ('student', 'Alyssa', 'P', 'Hacker', 'aphacker@mit.edu');
INSERT INTO people (type, first_name, middle_name, last_name, email) VALUES ('student', 'Eva', 'Lu', 'Ator', 'eval@mit.edu');
INSERT INTO people (type, first_name, middle_name, last_name, email) VALUES ('student', 'Louis', NULL, 'Reasoner', 'prolog@mit.edu');
INSERT INTO people (type, first_name, middle_name, last_name, email) VALUES ('professor', 'Gerald', 'Jay', 'Sussman', 'metacirc@mit.edu');
INSERT INTO people (type, first_name, middle_name, last_name, email) VALUES ('professor', 'Hal', NULL, 'Abelson', 'evalapply@mit.edu');

INSERT INTO classes (code, name) VALUES ('6001', 'The Structure and Interpretation of Computer Programs');
INSERT INTO classes (code, name) VALUES ('6946', 'The Structure and Interpretation of Classical Mechanics');
INSERT INTO classes (code, name) VALUES ('1806', 'Linear Algebra');

INSERT INTO registry (class_id, people_id) VALUES ((SELECT class_id FROM classes WHERE code = '6001'),
                                                   (SELECT people_id FROM people WHERE first_name = 'Gerald' AND middle_name = 'Jay' AND last_name = 'Sussman'));
INSERT INTO registry (class_id, people_id) VALUES ((SELECT class_id FROM classes WHERE code = '6946'),
                                                   (SELECT people_id FROM people WHERE first_name = 'Gerald' AND middle_name = 'Jay' AND last_name = 'Sussman'));
INSERT INTO registry (class_id, people_id) VALUES ((SELECT class_id FROM classes WHERE code = '6001'),
                                                   (SELECT people_id FROM people WHERE first_name = 'Hal' AND last_name = 'Abelson'));
