create schema if not exists catalog;

create table catalog.products
(
    id               uuid primary key,

    created_at       timestamp not null,
    created_by       varchar(255),
    last_modified    timestamp,
    last_modified_by varchar(255),

    name             varchar(255),
    description      text,
    image_file       varchar(255),
    price            numeric
);

create table catalog.product_categories
(
    product_id uuid not null,
    categories varchar(255),
    primary key (product_id, categories),
    constraint fk_product
        foreign key (product_id)
            references catalog.products (id)
            on delete cascade
);

create table catalog.outbox_events
(
    event_id         uuid primary key,
    occurred_on      timestamp    not null,
    event_type       varchar(255) not null,
    payload          text         not null,
    failed_attempts  int          not null default 0,
    next_attempt_at  timestamp    not null,
    last_error       text,
    dead_lettered_on timestamp,
    processed_on     timestamp
)