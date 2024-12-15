-- Insert data into the 'role' table
INSERT INTO role (id, created_date, last_modified_date, nom)
VALUES
    (1, NOW(), NOW(), 'ADMIN'),
    (2, NOW(), NOW(), 'MEMBER'),
    (3, NOW(), NOW(), 'OBSERVER')
ON CONFLICT (id) DO NOTHING;

-- Insert data into the '_user' table
INSERT INTO _user (id, created_date, email, last_modified_date, nom, password, prenom, role_id)
VALUES
    (1, NOW(), 'admin@example.com', NOW(), 'Admin', 'password123', 'John', 1),
    (2, NOW(), 'member1@example.com', NOW(), 'Member1', 'password123', 'Jane', 2),
    (3, NOW(), 'observer@example.com', NOW(), 'Observer', 'password123', 'Mike', 3)
ON CONFLICT (id) DO NOTHING;

-- Insert data into the 'project' table
INSERT INTO project (id, created_by, created_date, last_modified_by, last_modified_date, description, name, start_date, owner_id)
VALUES
    (1, 1, NOW(), 1, NOW(), 'Project Alpha Description', 'Project Alpha', '2024-01-01', 1),
    (2, 1, NOW(), 1, NOW(), 'Project Beta Description', 'Project Beta', '2024-01-15', 1)
ON CONFLICT (id) DO NOTHING;

-- Insert data into the 'project_members' table
INSERT INTO project_members (project_id, user_id)
VALUES
    (1, 1), -- Admin is a member of Project Alpha
    (1, 2), -- Member1 is a member of Project Alpha
    (2, 1), -- Admin is a member of Project Beta
    (2, 3)  -- Observer is a member of Project Beta
ON CONFLICT DO NOTHING;

-- Insert data into the 'tasks' table
INSERT INTO tasks (id, created_by, created_date, last_modified_by, last_modified_date, description, due_date, name, priority, status, assigned_to, project_id)
VALUES
    (1, 1, NOW(), 1, NOW(), 'Task 1 Description', '2024-02-01', 'Task 1', 0, 0, 2, 1), -- Priority 0 (LOW)
    (2, 1, NOW(), 1, NOW(), 'Task 2 Description', '2024-02-10', 'Task 2', 1, 1, 3, 1), -- Priority 1 (MEDIUM)
    (3, 1, NOW(), 1, NOW(), 'Task 3 Description', '2024-03-01', 'Task 3', 2, 2, 2, 1)  -- Priority 2 (HIGH)
ON CONFLICT (id) DO NOTHING;

-- Insert data into the 'task_modified_history' table
INSERT INTO task_modified_history (id, created_by, created_date, last_modified_by, last_modified_date, description, task_id, user_id)
VALUES
    (1, 1, NOW(), 1, NOW(), 'Initial creation of Task 1', 1, 2),
    (2, 1, NOW(), 1, NOW(), 'Initial creation of Task 2', 2, 3),
    (3, 1, NOW(), 1, NOW(), 'Initial creation of Task 3', 3, 2)
ON CONFLICT (id) DO NOTHING;
