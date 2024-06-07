INSERT INTO user(email, password, role)
VALUES ('misha@gmail.com', '$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 1);

INSERT INTO cities(name) VALUES ('Kiev'), ('Warsaw'), ('Berlin'), ('Paris'), ('Barcelona'), ('Rome'), ('Vienna');
INSERT INTO services(city, name) VALUES ('Kiev', 'Hotel');