create table idempotency_records (
    id bigint primary key auto_increment,
    scope varchar(50) not null,
    owner_id varchar(100) not null,
    idempotency_key varchar(200) not null,
    request_hash varchar(64) not null,
    status varchar(50) not null,
    response_body text null,
    created_at datetime not null,
    expires_at datetime not null,
    unique key uk_idempotency_records_scope_owner_key (scope, owner_id, idempotency_key)
);
