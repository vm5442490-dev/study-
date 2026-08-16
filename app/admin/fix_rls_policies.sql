-- =============================================================================
-- SUPER STUDY - SUPABASE RLS FIX SCRIPT
-- Run this script in Supabase Dashboard -> SQL Editor
-- This immediately fixes: "new row violates row-level security policy for table tests"
-- and allows full Admin CRUD (Insert, Update, Delete) + Public App Read access.
-- =============================================================================

-- Step 1: Drop restrictive authenticated-only policies if present
DROP POLICY IF EXISTS "Admin all classes" ON public.classes;
DROP POLICY IF EXISTS "Admin all subjects" ON public.subjects;
DROP POLICY IF EXISTS "Admin all chapters" ON public.chapters;
DROP POLICY IF EXISTS "Admin all books" ON public.books;
DROP POLICY IF EXISTS "Admin all notes" ON public.notes;
DROP POLICY IF EXISTS "Admin all pdf_documents" ON public.pdf_documents;
DROP POLICY IF EXISTS "Admin all model_papers" ON public.model_papers;
DROP POLICY IF EXISTS "Admin all previous_year_papers" ON public.previous_year_papers;
DROP POLICY IF EXISTS "Admin all questions" ON public.questions;
DROP POLICY IF EXISTS "Admin all tests" ON public.tests;
DROP POLICY IF EXISTS "Admin all announcements" ON public.announcements;
DROP POLICY IF EXISTS "Admin all daily_updates" ON public.daily_updates;
DROP POLICY IF EXISTS "Admin all leaderboard" ON public.leaderboard;
DROP POLICY IF EXISTS "Admin all test_attempts" ON public.test_attempts;

-- Step 2: Grant full write access (INSERT, UPDATE, DELETE) for both anon (Web Panel) and authenticated users
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

-- Step 3: Ensure Storage bucket 'pdf-documents' allows public uploads
INSERT INTO storage.buckets (id, name, public) 
VALUES ('pdf-documents', 'pdf-documents', true) 
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Public Storage Uploads" ON storage.objects;
DROP POLICY IF EXISTS "Public Storage Downloads" ON storage.objects;

CREATE POLICY "Public Storage Uploads" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'pdf-documents');
CREATE POLICY "Public Storage Downloads" ON storage.objects FOR SELECT USING (bucket_id = 'pdf-documents');
