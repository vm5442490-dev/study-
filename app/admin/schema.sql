-- =============================================================================
-- SUPER STUDY - COMPLETE SUPABASE POSTGRESQL SCHEMA & SEED SCRIPT
-- Copy and paste this script into your Supabase Dashboard -> SQL Editor and run it.
-- =============================================================================

-- 1. CLASSES TABLE
CREATE TABLE IF NOT EXISTS public.classes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    order_index INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 2. SUBJECTS TABLE
CREATE TABLE IF NOT EXISTS public.subjects (
    id TEXT PRIMARY KEY,
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    icon_name TEXT DEFAULT 'book',
    color_hex TEXT DEFAULT '#1E40AF',
    code TEXT DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 3. CHAPTERS TABLE
CREATE TABLE IF NOT EXISTS public.chapters (
    id TEXT PRIMARY KEY,
    subject_id TEXT NOT NULL REFERENCES public.subjects(id) ON DELETE CASCADE,
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    chapter_number INTEGER DEFAULT 1,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 4. BOOKS TABLE
CREATE TABLE IF NOT EXISTS public.books (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT DEFAULT 'NCERT Books',
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    subject_id TEXT REFERENCES public.subjects(id) ON DELETE SET NULL,
    subject_name TEXT DEFAULT '',
    cover_url TEXT,
    pdf_url TEXT,
    chapters_count INTEGER DEFAULT 0,
    author TEXT DEFAULT 'NCERT / State Board',
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 5. STUDY NOTES TABLE
CREATE TABLE IF NOT EXISTS public.notes (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    subject_id TEXT REFERENCES public.subjects(id) ON DELETE SET NULL,
    subject_name TEXT DEFAULT '',
    chapter_id TEXT REFERENCES public.chapters(id) ON DELETE SET NULL,
    chapter_title TEXT DEFAULT '',
    content TEXT DEFAULT '',
    summary TEXT DEFAULT '',
    key_points JSONB DEFAULT '[]'::jsonb,
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 6. PDF DOCUMENTS TABLE
CREATE TABLE IF NOT EXISTS public.pdf_documents (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    category TEXT DEFAULT 'Study Material',
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    subject_id TEXT REFERENCES public.subjects(id) ON DELETE SET NULL,
    subject_name TEXT DEFAULT '',
    file_url TEXT NOT NULL,
    pages_count INTEGER DEFAULT 10,
    file_size TEXT DEFAULT '2.4 MB',
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 7. 2026 MODEL PAPERS TABLE
CREATE TABLE IF NOT EXISTS public.model_papers (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    subject_id TEXT REFERENCES public.subjects(id) ON DELETE SET NULL,
    subject_name TEXT DEFAULT '',
    year TEXT DEFAULT '2026',
    questions_count INTEGER DEFAULT 20,
    duration_minutes INTEGER DEFAULT 90,
    file_url TEXT,
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 8. PREVIOUS YEAR PAPERS (PYQ) TABLE
CREATE TABLE IF NOT EXISTS public.previous_year_papers (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    subject_id TEXT REFERENCES public.subjects(id) ON DELETE SET NULL,
    subject_name TEXT DEFAULT '',
    year TEXT DEFAULT '2025',
    exam_type TEXT DEFAULT 'Annual Board Exam',
    questions_count INTEGER DEFAULT 30,
    file_url TEXT,
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 9. MASTER QUESTION BANK (WITH IMGBB DIAGRAM SUPPORT)
CREATE TABLE IF NOT EXISTS public.questions (
    id TEXT PRIMARY KEY,
    question_text TEXT NOT NULL,
    image_url TEXT,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_option TEXT NOT NULL DEFAULT 'A',
    explanation TEXT DEFAULT '',
    difficulty TEXT DEFAULT 'Medium',
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    subject_id TEXT REFERENCES public.subjects(id) ON DELETE SET NULL,
    subject_name TEXT DEFAULT '',
    chapter_id TEXT REFERENCES public.chapters(id) ON DELETE SET NULL,
    chapter_title TEXT DEFAULT '',
    question_type TEXT DEFAULT 'MCQ',
    marks INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 10. ONLINE TESTS & DAILY LIVE QUIZZES
CREATE TABLE IF NOT EXISTS public.tests (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    class_id TEXT NOT NULL REFERENCES public.classes(id) ON DELETE CASCADE,
    subject_id TEXT DEFAULT '',
    subject_name TEXT DEFAULT '',
    duration_minutes INTEGER DEFAULT 15,
    total_marks INTEGER DEFAULT 20,
    passing_marks INTEGER DEFAULT 8,
    questions_count INTEGER DEFAULT 10,
    is_daily_quiz BOOLEAN DEFAULT false,
    is_featured BOOLEAN DEFAULT false,
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 11. TEST ATTEMPTS & ANALYTICS
CREATE TABLE IF NOT EXISTS public.test_attempts (
    id TEXT PRIMARY KEY,
    test_id TEXT NOT NULL,
    test_title TEXT DEFAULT '',
    student_name TEXT NOT NULL,
    student_class TEXT DEFAULT '',
    score INTEGER DEFAULT 0,
    total_marks INTEGER DEFAULT 0,
    correct_count INTEGER DEFAULT 0,
    wrong_count INTEGER DEFAULT 0,
    skipped_count INTEGER DEFAULT 0,
    accuracy_percentage NUMERIC DEFAULT 0.0,
    time_taken_seconds INTEGER DEFAULT 0,
    rank INTEGER DEFAULT 1,
    completed_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 12. ANNOUNCEMENTS & FLASH BANNERS
CREATE TABLE IF NOT EXISTS public.announcements (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    badge TEXT DEFAULT 'UPDATE',
    link TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 13. DAILY UPDATES & NEWS FEED
CREATE TABLE IF NOT EXISTS public.daily_updates (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT DEFAULT '',
    tag TEXT DEFAULT 'UPDATE',
    date TEXT DEFAULT '',
    is_pinned BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- 14. LEADERBOARD
CREATE TABLE IF NOT EXISTS public.leaderboard (
    id TEXT PRIMARY KEY,
    student_name TEXT NOT NULL,
    student_class TEXT DEFAULT '',
    score INTEGER DEFAULT 0,
    accuracy NUMERIC DEFAULT 0.0,
    time_taken_seconds INTEGER DEFAULT 0,
    rank INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now())
);

-- =============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES - OPEN FOR STUDENT READ & ADMIN WRITE (ANON + AUTH)
-- =============================================================================

ALTER TABLE public.classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subjects ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chapters ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.books ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pdf_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.model_papers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.previous_year_papers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.test_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_updates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leaderboard ENABLE ROW LEVEL SECURITY;

-- Allow full access (SELECT, INSERT, UPDATE, DELETE) for all operational tables
CREATE POLICY "Full access classes" ON public.classes FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access subjects" ON public.subjects FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access chapters" ON public.chapters FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access books" ON public.books FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access notes" ON public.notes FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access pdf_documents" ON public.pdf_documents FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access model_papers" ON public.model_papers FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access previous_year_papers" ON public.previous_year_papers FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access questions" ON public.questions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access tests" ON public.tests FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access announcements" ON public.announcements FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access daily_updates" ON public.daily_updates FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access leaderboard" ON public.leaderboard FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Full access test_attempts" ON public.test_attempts FOR ALL USING (true) WITH CHECK (true);

-- =============================================================================
-- SEED INITIAL DATA
-- =============================================================================

INSERT INTO public.classes (id, name, code, order_index) VALUES
('class-12', 'Class 12th', '12', 1),
('class-11', 'Class 11th', '11', 2),
('class-10', 'Class 10th', '10', 3),
('class-9', 'Class 9th', '9', 4)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.subjects (id, class_id, name, icon_name, color_hex, code) VALUES
('sub-hist-12', 'class-12', 'History (इतिहास)', 'history', '#1E40AF', 'HIST'),
('sub-geo-12', 'class-12', 'Geography (भूगोल)', 'public', '#0D9488', 'GEO'),
('sub-pol-12', 'class-12', 'Political Science (राजनीति)', 'account_balance', '#7C3AED', 'POL'),
('sub-phys-12', 'class-12', 'Physics (भौतिकी)', 'bolt', '#EA580C', 'PHY'),
('sub-chem-12', 'class-12', 'Chemistry (रसायन)', 'science', '#D97706', 'CHEM'),
('sub-bio-12', 'class-12', 'Biology (जीव विज्ञान)', 'eco', '#059669', 'BIO'),
('sub-math-12', 'class-12', 'Mathematics (गणित)', 'calculate', '#2563EB', 'MATH'),
('sub-sci-10', 'class-10', 'Science (विज्ञान)', 'science', '#16A34A', 'SCI'),
('sub-math-10', 'class-10', 'Mathematics (गणित)', 'calculate', '#2563EB', 'MATH'),
('sub-hin-10', 'class-10', 'Hindi (हिंदी)', 'menu_book', '#DC2626', 'HIN')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.announcements (id, title, description, badge, is_active) VALUES
('ann-1', '🎯 Board Exam 2026 Model Question Papers Released!', 'कक्षा 10वीं और 12वीं के सभी विषयों के 2026 मॉडल पेपर्स PDF सेक्शन में उपलब्ध हैं। अभी डाउनलोड करें।', 'HOT', true),
('ann-2', '⚡ Daily Live Quiz is Live!', 'प्रतिदिन 10 नए प्रश्नों का अभ्यास करें और राज्य स्तरीय लीडरबोर्ड में टॉप रैंक प्राप्त करें।', 'DAILY', true),
('ann-3', '📚 New Handwritten Notes Added for Class 12 History & Science', 'इतिहास और भौतिकी के नए संक्षिप्त रिवीजन नोट्स और सूत्र तालिका अपडेट कर दी गई है।', 'NEW', true)
ON CONFLICT (id) DO NOTHING;
