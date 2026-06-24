create table users (
                       id bigint primary key auto_increment,
                       email varchar(255) not null unique,
                       full_name varchar(255) not null,
                       created_at datetime not null,
                       updated_at datetime not null
);