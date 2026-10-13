-- ============================================================================
-- HISABTRIP PRODUCTION DATABASE SCHEMA & ROW LEVEL SECURITY (RLS) POLICIES
-- PostgreSQL / Supabase
-- Currency Unit: Integer minor units (INR paise: ₹1.00 = 100 paise)
-- ============================================================================

-- 1. Profiles (Immutable user ID mapped to auth.uid())
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    user_id TEXT UNIQUE NOT NULL,
    email TEXT NOT NULL,
    display_name TEXT NOT NULL,
    username TEXT UNIQUE NOT NULL,
    avatar_key TEXT NOT NULL DEFAULT 'compass',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Trips
CREATE TABLE IF NOT EXISTS public.trips (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    start_date TEXT DEFAULT '',
    end_date TEXT DEFAULT '',
    currency_code TEXT NOT NULL DEFAULT 'INR',
    budget_minor BIGINT NOT NULL DEFAULT 0, -- integer minor units (paise)
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'DELETED')),
    created_by TEXT NOT NULL REFERENCES public.profiles(user_id),
    deleted_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Trip Members
CREATE TABLE IF NOT EXISTS public.trip_members (
    trip_id TEXT NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES public.profiles(user_id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('CREATOR', 'MANAGER', 'MEMBER')),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED')),
    target_minor BIGINT NOT NULL DEFAULT 0, -- contribution target in paise
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (trip_id, user_id)
);

-- 4. Trip Invitations
CREATE TABLE IF NOT EXISTS public.trip_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id TEXT NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    invited_by TEXT NOT NULL REFERENCES public.profiles(user_id),
    invite_code TEXT UNIQUE NOT NULL,
    expires_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED'))
);

-- 5. Wallet Contributions
CREATE TABLE IF NOT EXISTS public.contributions (
    id TEXT PRIMARY KEY,
    trip_id TEXT NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES public.profiles(user_id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    recorded_by TEXT NOT NULL REFERENCES public.profiles(user_id),
    recorded_at TIMESTAMPTZ DEFAULT NOW(),
    note TEXT DEFAULT ''
);

-- 6. Expenses
CREATE TABLE IF NOT EXISTS public.expenses (
    id TEXT PRIMARY KEY,
    trip_id TEXT NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    category TEXT NOT NULL DEFAULT 'Food & Dining',
    location TEXT DEFAULT '',
    description TEXT DEFAULT '',
    expense_date TIMESTAMPTZ DEFAULT NOW(),
    status TEXT NOT NULL DEFAULT 'APPROVED' CHECK (status IN ('APPROVED', 'PENDING', 'REJECTED')),
    created_by TEXT NOT NULL REFERENCES public.profiles(user_id),
    approved_by TEXT REFERENCES public.profiles(user_id),
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    receipt_note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Expense Payers (Multi-payer support)
CREATE TABLE IF NOT EXISTS public.expense_payers (
    expense_id TEXT NOT NULL REFERENCES public.expenses(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES public.profiles(user_id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    PRIMARY KEY (expense_id, user_id)
);

-- 8. Expense Participants (Split shares)
CREATE TABLE IF NOT EXISTS public.expense_participants (
    expense_id TEXT NOT NULL REFERENCES public.expenses(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL REFERENCES public.profiles(user_id),
    share_minor BIGINT NOT NULL CHECK (share_minor >= 0),
    PRIMARY KEY (expense_id, user_id)
);

-- 9. Settlements
CREATE TABLE IF NOT EXISTS public.settlements (
    id TEXT PRIMARY KEY,
    trip_id TEXT NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    from_user_id TEXT NOT NULL REFERENCES public.profiles(user_id),
    to_user_id TEXT NOT NULL REFERENCES public.profiles(user_id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID')),
    paid_at TIMESTAMPTZ,
    payment_note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. Audit Log
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id TEXT NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    actor_user_id TEXT NOT NULL REFERENCES public.profiles(user_id),
    action TEXT NOT NULL,
    details TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 11. In-App Notifications
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL REFERENCES public.profiles(user_id),
    trip_id TEXT REFERENCES public.trips(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trips ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trip_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.contributions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expense_payers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expense_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.settlements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- Helper function: Check if current auth user is an active member of the trip
CREATE OR REPLACE FUNCTION public.is_member_of_trip(trip_id_arg TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.trip_members tm
        JOIN public.profiles p ON tm.user_id = p.user_id
        WHERE tm.trip_id = trip_id_arg
        AND p.id = auth.uid()
        AND tm.status = 'ACTIVE'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Profiles: Anyone can view public profile info; only owner can update
CREATE POLICY "Profiles readable by authenticated users" ON public.profiles
    FOR SELECT TO authenticated USING (true);

CREATE POLICY "Users can update own profile" ON public.profiles
    FOR UPDATE TO authenticated USING (id = auth.uid());

-- Trips: Only active members can select
CREATE POLICY "Trips viewable by members" ON public.trips
    FOR SELECT TO authenticated USING (public.is_member_of_trip(id));

CREATE POLICY "Users can create trips" ON public.trips
    FOR INSERT TO authenticated WITH CHECK (
        created_by IN (SELECT user_id FROM public.profiles WHERE id = auth.uid())
    );

-- Expenses: Only trip members can view and insert
CREATE POLICY "Expenses viewable by trip members" ON public.expenses
    FOR SELECT TO authenticated USING (public.is_member_of_trip(trip_id));

CREATE POLICY "Expenses insertable by trip members" ON public.expenses
    FOR INSERT TO authenticated WITH CHECK (public.is_member_of_trip(trip_id));

-- RPC: Join Trip by Code safely
CREATE OR REPLACE FUNCTION public.join_trip_by_code(code_arg TEXT)
RETURNS TEXT AS $$
DECLARE
    found_trip_id TEXT;
    caller_user_id TEXT;
BEGIN
    SELECT user_id INTO caller_user_id FROM public.profiles WHERE id = auth.uid();
    IF caller_user_id IS NULL THEN
        RAISE EXCEPTION 'User profile not found';
    END IF;

    SELECT id INTO found_trip_id FROM public.trips WHERE id = code_arg;
    IF found_trip_id IS NULL THEN
        RAISE EXCEPTION 'Invalid trip code';
    END IF;

    INSERT INTO public.trip_members(trip_id, user_id, role, status, target_minor)
    VALUES (found_trip_id, caller_user_id, 'MEMBER', 'ACTIVE', 0)
    ON CONFLICT (trip_id, user_id) DO UPDATE SET status = 'ACTIVE';

    RETURN found_trip_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
