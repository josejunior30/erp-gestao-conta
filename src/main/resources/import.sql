

INSERT INTO tb_role (authority ) VALUES ( 'ROLE_ADMIN');
INSERT INTO tb_role (authority ) VALUES ( 'ROLE_OPERADOR');

INSERT INTO tb_user (first_name, last_name, email, password) VALUES ('Junior','Junior' ,'joseluizjunior@yahoo.com', '$2a$10$VYmnVUxkC1hyshP5SzWeXuq0K/glYVkHM92DSt/vV/xhzAFyfULBK');
INSERT INTO tb_user_role (user_id, role_id ) VALUES ( 1, 1);
