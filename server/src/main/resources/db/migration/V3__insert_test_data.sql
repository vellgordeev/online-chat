INSERT INTO users (login, password, username, role)
VALUES
    ('user1', crypt('pass1', gen_salt('bf')), 'User1', 'USER'),
    ('user2', crypt('pass2', gen_salt('bf')), 'User2', 'USER'),
    ('admin', crypt('adminpass', gen_salt('bf')), 'AdminUser', 'ADMIN');