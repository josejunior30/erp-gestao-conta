

INSERT INTO tb_role (authority ) VALUES ( 'ROLE_ADMIN');
INSERT INTO tb_role (authority ) VALUES ( 'ROLE_OPERADOR');

INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Junior','Ribeiro Junior' ,'joseluizjunior@yahoo.com', '$2a$10$7fUHpqBMkb8xzdigZvJo2eOmAitjGOcKV5YYaMjm6C/8AageW5pPe');
INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Jaline','Mirian Peereira' ,'jalineemirian@gmail.com', '$2a$10$7fUHpqBMkb8xzdigZvJo2eOmAitjGOcKV5YYaMjm6C/8AageW5pPe');

INSERT INTO tb_user_role (user_id, role_id ) VALUES ( 1, 1);
INSERT INTO tb_user_role (user_id, role_id ) VALUES ( 2, 2);