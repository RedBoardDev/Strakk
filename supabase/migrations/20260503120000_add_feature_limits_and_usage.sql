-- =============================================================================
-- Feature limits (config) + Feature usage (log)
-- Replaces ad-hoc quota counting from meal_entries / check_ins / KV.
-- =============================================================================

-- 1. Feature limits — one row per feature, controls PRO gating + quotas + rate limits
create table if not exists public.feature_limits (
  feature_key   text primary key,
  pro_only      boolean   not null default true,
  quota_free    integer   not null default 0,
  quota_pro     integer   not null default -1,   -- -1 = unlimited
  quota_period  text      not null default 'month' check (quota_period in ('day', 'week', 'month')),
  rate_limit_max       integer not null default 0,  -- 0 = no rate limit
  rate_limit_window_s  integer not null default 0,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);

comment on table public.feature_limits is 'Per-feature configuration: PRO gating, quotas, rate limits.';
comment on column public.feature_limits.quota_pro is '-1 means unlimited quota for PRO users.';
comment on column public.feature_limits.rate_limit_max is '0 means no rate limit enforced.';

-- 2. Feature usage — one row per feature invocation, used for quota counting
create table if not exists public.feature_usage (
  id          bigint generated always as identity primary key,
  user_id     uuid        not null references auth.users(id) on delete cascade,
  feature_key text        not null references public.feature_limits(feature_key),
  created_at  timestamptz not null default now()
);

create index if not exists idx_feature_usage_user_feature_date
  on public.feature_usage (user_id, feature_key, created_at desc);

comment on table public.feature_usage is 'Usage log per user per feature. Used for quota enforcement.';

-- 3. RLS
alter table public.feature_limits enable row level security;
alter table public.feature_usage enable row level security;

-- feature_limits: readable by everyone (config is not secret), writable by nobody via API
create policy "feature_limits_read" on public.feature_limits
  for select using (true);

-- feature_usage: users can read their own usage (for client-side display)
create policy "feature_usage_read_own" on public.feature_usage
  for select using (auth.uid() = user_id);

-- feature_usage inserts are done via service_role from Edge Functions, not via client.
-- No insert policy for authenticated users — enforces server-side only writes.

-- 4. Seed data
insert into public.feature_limits (feature_key, pro_only, quota_free, quota_pro, quota_period, rate_limit_max, rate_limit_window_s)
values
  ('ai_photo_analysis',  true,  0, 100, 'month', 10, 60),
  ('ai_text_analysis',   true,  0, 100, 'month', 10, 60),
  ('ai_weekly_summary',  true,  0,   5, 'month',  3, 60),
  ('health_sync',        true,  0,  -1, 'month',  5, 60),
  ('unlimited_history',  true,  0,  -1, 'month',  0,  0),
  ('photo_comparison',   true,  0,  -1, 'month',  5, 60),
  ('hevy_export',        true,  0,   2, 'month',  2, 60)
on conflict (feature_key) do nothing;

-- 5. Helper: count usage for a user+feature within a period
create or replace function public.count_feature_usage(
  p_user_id     uuid,
  p_feature_key text,
  p_since       timestamptz
)
returns integer
language sql
stable
security definer
as $$
  select coalesce(count(*)::integer, 0)
  from public.feature_usage
  where user_id = p_user_id
    and feature_key = p_feature_key
    and created_at >= p_since;
$$;

-- 6. Updated_at trigger for feature_limits
create or replace function public.update_feature_limits_timestamp()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger trg_feature_limits_updated_at
  before update on public.feature_limits
  for each row execute function public.update_feature_limits_timestamp();
