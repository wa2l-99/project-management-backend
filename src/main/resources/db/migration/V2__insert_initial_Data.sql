-- Insertion des rôles avec created_by
INSERT INTO role (nom, created_date)
VALUES
    ('ADMIN', NOW()),
    ('MEMBER', NOW()),
    ('OBSERVER', NOW())
ON CONFLICT (id) DO NOTHING;

-- Insertion des utilisateurs
INSERT INTO _user (nom, prenom, email, password,role_id, created_date)
VALUES
    ('Doe', 'John', 'john.doe@example.com', '$2a$10$XvA0u7xinasa9s6Qu32w9ucVkszhCyASKpAUnJ8HjqHOuM6ROQjey',1, NOW()), --Mot de passe = password123
    ('Smith', 'Jane', 'jane.smith@example.com', '$2a$10$XvA0u7xinasa9s6Qu32w9ucVkszhCyASKpAUnJ8HjqHOuM6ROQjey',null, NOW()), --Mot de passe = password123
    ('Brown', 'Morgan', 'morgan.brown@example.com', '$2a$10$XvA0u7xinasa9s6Qu32w9ucVkszhCyASKpAUnJ8HjqHOuM6ROQjey',null, NOW()) --Mot de passe = password123
ON CONFLICT (email) DO NOTHING;


-- Insertion des projets avec created_by
INSERT INTO project (id, name, description, start_date, owner_id, created_date, created_by) VALUES
    (200, 'Website Redesign', 'Revamping the corporate website with a modern design.', '2025-01-01', 1, NOW(), 1),
    (201, 'Mobile App Development', 'Building a cross-platform mobile app for e-commerce.', '2024-12-20', 1, NOW(), 1),
    (202, 'Cloud Migration', 'Migrating on-premise infrastructure to a cloud platform.', '2025-02-15', 1, NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- Ajout des membres aux projets
INSERT INTO project_members (project_id, user_id)
VALUES
    (200, 1),
    (201, 1),
    (202, 1),
    (201, 2),
    (202, 3);

-- Insertion des tâches avec created_by
INSERT INTO tasks (id, name, description, due_date, priority, status, project_id,assigned_to, created_date, created_by)
VALUES
    (280, 'Design Homepage', 'Create a responsive design for the homepage.', '2025-01-10', 0, 0, 200, null, NOW(), 1),
    (281, 'Develop Shopping Cart', 'Implement the shopping cart feature for the app.', '2025-01-25', 1, 0, 201, null, NOW(), 1),
    (282, 'Test Payment Gateway', 'Ensure the payment gateway integrates seamlessly.', '2025-01-30', 2, 0, 201, null, NOW(), 1),
    (283, 'Set Up Cloud Infrastructure', 'Create and configure resources in the cloud platform.', '2025-02-20', 2, 0, 202, null, NOW(), 1),
    (284, 'Data Migration', 'Migrate data from on-premise to cloud storage.', '2025-03-05', 0, 0, 202, null, NOW(), 1)
ON CONFLICT (id) DO NOTHING;

-- Insertion d'historique des modifications avec created_by
INSERT INTO task_modified_history (id, task_id, user_id, description, created_date, created_by)
VALUES
    (300, 280, 1, 'Nom modifié de ''Homepage Design Version 2'' à ''Design Homepage''. Description modifiée de ''Add animations and mobile optimizations.'' à ''Create a responsive design for the homepage.''. Date d''échéance modifiée de ''2025-01-20'' à ''2025-01-25''.', NOW(), 1),
    (301, 281, 2, 'Statut modifié de ''IN_PROGRESS'' à ''TODO''. Priorité modifiée de ''LOW'' à ''MEDIUM''.', NOW(), 2),
    (302, 282, 3, 'Description modifiée de ''Test additional edge cases for payment gateway.'' à ''Ensure the payment gateway integrates seamlessly''.', NOW(), 2),
    (303, 283, 1, 'Nom modifié de ''Initial Cloud Setup'' à ''Set Up Cloud Infrastructure''. Description modifiée de ''Set up the foundational cloud architecture.'' à ''Create and configure resources in the cloud platform.''.', NOW(), 1),
    (304, 284, 3, 'Date d''échéance modifiée de ''2025-03-01'' à ''2025-03-05''.', NOW(), 1)
ON CONFLICT (id) DO NOTHING;

