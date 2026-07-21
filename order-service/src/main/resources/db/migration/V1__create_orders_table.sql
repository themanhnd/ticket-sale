create table orders (
                        id bigint primary key auto_increment,
                        order_no varchar(100) not null unique,
                        user_id bigint not null,
                        event_id bigint not null,
                        quantity int not null,
                        status varchar(50) not null,
                        expires_at datetime not null,
                        created_at datetime not null,
                        updated_at datetime not null
);