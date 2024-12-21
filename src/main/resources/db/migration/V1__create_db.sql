CREATE TABLE IF NOT EXISTS role (
                      id SERIAL PRIMARY KEY,
                      nom VARCHAR(20) NOT NULL,
                      created_date TIMESTAMP NOT NULL DEFAULT NOW(),
                      last_modified_date TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS _user (
                       id SERIAL PRIMARY KEY,
                       nom VARCHAR(255) NOT NULL,
                       prenom VARCHAR(255) NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       role_id INTEGER REFERENCES role(id) ON DELETE SET NULL,
                       created_date TIMESTAMP NOT NULL DEFAULT NOW(),
                       last_modified_date TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS project (
                         id SERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         description VARCHAR(500),
                         start_date DATE NOT NULL,
                         owner_id INTEGER REFERENCES _user(id) ON DELETE SET NULL,
                         created_date TIMESTAMP NOT NULL DEFAULT NOW(),
                         last_modified_date TIMESTAMP DEFAULT NOW(),
                         last_modified_by INT,
                         created_by INTEGER REFERENCES _user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS project_members (
                                 project_id INTEGER REFERENCES project(id) ON DELETE CASCADE,
                                 user_id INTEGER REFERENCES _user(id) ON DELETE CASCADE,
                                 PRIMARY KEY (project_id, user_id)
);

CREATE TABLE IF NOT EXISTS tasks (
                       id SERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description VARCHAR(500),
                       due_date DATE NOT NULL,
                       priority SMALLINT NOT NULL,
                       status SMALLINT NOT NULL,
                       project_id INTEGER REFERENCES project(id) ON DELETE CASCADE,
                       assigned_to INTEGER REFERENCES _user(id) ON DELETE SET NULL,
                       created_date TIMESTAMP NOT NULL DEFAULT NOW(),
                       last_modified_date TIMESTAMP DEFAULT NOW(),
                       last_modified_by INT,
                       created_by INTEGER REFERENCES _user(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS task_modified_history (
                                       id SERIAL PRIMARY KEY,
                                       task_id INTEGER REFERENCES tasks(id) ON DELETE CASCADE,
                                       user_id INTEGER REFERENCES _user(id) ON DELETE CASCADE,
                                       description VARCHAR(255),
                                       created_date TIMESTAMP NOT NULL DEFAULT NOW(),
                                       last_modified_by INT,
                                       last_modified_date TIMESTAMP DEFAULT NOW(),
                                       created_by INTEGER REFERENCES _user(id) ON DELETE SET NULL
);
