-- Demo catalog for the SETUP.md quickstart. Applied by catalogSeedFlyway, a Flyway instance
-- separate from the schema one and keeping its own flyway_seed_history table, so this file is
-- invisible to anything migrating db/catalog alone — including every catalog test.
--
-- Setting eshop.catalog.seed-demo-data back to false is therefore safe: the schema's history has
-- no record of this migration, so nothing reports it as missing. What it does NOT do is remove
-- rows already inserted — the flag controls whether the seed runs, never whether the data is
-- there. An empty catalog needs a fresh database (docker compose down -v).
--
-- Ids are fixed so SETUP.md can quote one in its add-to-basket call.

insert into catalog.products (id, created_at, created_by, name, description, image_file, price)
values
    ('a1000000-0000-4000-8000-000000000001', timestamp '2026-01-01 00:00:00', 'seed',
     'Galaxy Smartphone', 'Flagship handset with a 6.8" display', 'galaxy.png', 550.00),
    ('a1000000-0000-4000-8000-000000000002', timestamp '2026-01-01 00:00:00', 'seed',
     'Aurora Laptop', '14" ultrabook, 32GB RAM', 'aurora.png', 1450.00),
    ('a1000000-0000-4000-8000-000000000003', timestamp '2026-01-01 00:00:00', 'seed',
     'Pulse Headphones', 'Over-ear, active noise cancelling', 'pulse.png', 220.00)
on conflict (id) do nothing;

insert into catalog.product_categories (product_id, categories)
values
    ('a1000000-0000-4000-8000-000000000001', 'Smartphones'),
    ('a1000000-0000-4000-8000-000000000002', 'Computers'),
    ('a1000000-0000-4000-8000-000000000003', 'Audio'),
    ('a1000000-0000-4000-8000-000000000003', 'Accessories')
on conflict do nothing;