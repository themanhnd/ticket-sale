create table events (
                        id bigint not null auto_increment,
                        code varchar(100) not null,
                        name varchar(255) not null,
                        location varchar(255) not null,
                        created_at datetime(6) not null,
                        updated_at datetime(6) not null,
                        primary key (id),
                        unique key uk_events_code (code)
);