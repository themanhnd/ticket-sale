create table templates (
                           id bigint primary key auto_increment,
                           code varchar(100) not null unique,
                           name varchar(255) not null,
                           created_at datetime not null,
                           updated_at datetime not null
);