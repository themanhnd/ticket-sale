create table inventories (
                             id bigint not null auto_increment,
                             event_id bigint not null,
                             total_quantity int not null,
                             available_quantity int not null,
                             created_at datetime(6) not null,
                             updated_at datetime(6) not null,
                             primary key (id),
                             unique key uk_inventories_event_id (event_id)
);