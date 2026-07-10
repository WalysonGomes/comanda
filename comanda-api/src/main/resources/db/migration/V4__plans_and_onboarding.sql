-- plans-and-onboarding: aligns tenants.plan values with the Plan enum (GRATUITO/ESSENCIAL, task 1.2)
-- and adds the year-month competency marker used for the lazy monthly reset of order_count_month
-- (design.md Decision 3 / task 1.5) — no cron dependency, correct even without a job running
-- exactly at midnight on the 1st.
UPDATE tenants SET plan = 'GRATUITO' WHERE plan = 'FREE';
ALTER TABLE tenants ALTER COLUMN plan SET DEFAULT 'GRATUITO';

ALTER TABLE tenants ADD COLUMN order_count_month_period VARCHAR(7);
UPDATE tenants SET order_count_month_period = to_char(now() AT TIME ZONE 'America/Fortaleza', 'YYYY-MM');
ALTER TABLE tenants ALTER COLUMN order_count_month_period SET NOT NULL;
ALTER TABLE tenants ALTER COLUMN order_count_month_period SET DEFAULT to_char(now() AT TIME ZONE 'America/Fortaleza', 'YYYY-MM');
