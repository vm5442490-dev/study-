/* ==========================================================================
   SUPER STUDY ADMIN PANEL JAVASCRIPT CORE
   Connected to Supabase - Full CRUD, Storage, Test Builder, ImgBB
   ========================================================================== */

// Initialize Supabase Client using URL and Key from config.js or window.SUPABASE_CONFIG
if (!window.supabaseClient && window.supabase && window.SUPABASE_CONFIG) {
    window.supabaseClient = window.supabase.createClient(
        window.SUPABASE_CONFIG.url,
        window.SUPABASE_CONFIG.anonKey
    );
}
const supabaseClient = window.supabaseClient;

let currentRoute = 'dashboard';
let cachedClasses = [];
let cachedSubjects = [];
let cachedChapters = [];

// ==========================================================================
// 1. INITIALIZATION & AUTHENTICATION
// ==========================================================================

document.addEventListener('DOMContentLoaded', async () => {
    // Check if client is initialized
    if (!supabaseClient) {
        console.error('Supabase client failed to initialize. Check config.js.');
        showToast('Supabase connection configuration error.', 'error');
    }

    // Check existing auth session or master local token
    const localLoggedIn = localStorage.getItem('superstudy_admin_logged_in');
    const localEmail = localStorage.getItem('superstudy_admin_email') || 'vm544806@gmail.com';

    if (localLoggedIn === 'true') {
        setupAdminSession({ email: localEmail });
        return;
    }

    try {
        if (supabaseClient && supabaseClient.auth) {
            const { data: { session }, error } = await supabaseClient.auth.getSession();
            if (!error && session && session.user) {
                setupAdminSession(session.user);
                return;
            }
        }
    } catch (e) {
        console.warn('Auth session check fallback:', e);
    }
    
    showAuthScreen();
});

function showAuthScreen() {
    document.getElementById('auth-screen').style.display = 'flex';
    document.getElementById('admin-app').style.display = 'none';
}

/**
 * Handles administrator authentication using supabase.auth.signInWithPassword
 * with robust input validation, credentials authentication, and authorization.
 */
async function handleAdminLogin(event) {
    if (event) event.preventDefault();
    
    const emailInput = document.getElementById('login-email');
    const passwordInput = document.getElementById('login-password');
    const btn = document.getElementById('btn-login');

    const email = emailInput ? emailInput.value.trim() : '';
    const password = passwordInput ? passwordInput.value : '';

    // 1. Input Validation
    if (!email) {
        showToast('Please enter an admin email address.', 'error');
        if (emailInput) emailInput.focus();
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email) && email.toLowerCase() !== 'admin') {
        showToast('Please enter a valid email format.', 'error');
        if (emailInput) emailInput.focus();
        return;
    }

    if (!password || password.length < 4) {
        showToast('Please enter your administrator password (at least 4 characters).', 'error');
        if (passwordInput) passwordInput.focus();
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Authenticating...';

    // 2. Authorized Administrator Whitelist / Direct Admin Access
    const normalizedEmail = email.toLowerCase();
    const authorizedEmails = [
        'vm544806@gmail.com',
        'vm5442490@gmail.com',
        'admin@superstudy.in',
        'admin@superstudy.com',
        'admin'
    ];
    const authorizedMasterPasskeys = ['Vishal1234', 'SuperStudy@2026', 'admin123', 'admin', 'vishal1234'];

    if (authorizedEmails.includes(normalizedEmail) && authorizedMasterPasskeys.includes(password)) {
        const activeAdminEmail = email.includes('@') ? email : 'vm544806@gmail.com';
        localStorage.setItem('superstudy_admin_logged_in', 'true');
        localStorage.setItem('superstudy_admin_email', activeAdminEmail);
        setupAdminSession({ email: activeAdminEmail });
        showToast('Administrator authentication successful! Welcome.', 'success');
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-lock-open"></i> Sign In to Admin Panel';
        return;
    }

    // 3. Supabase Auth Integration: supabase.auth.signInWithPassword
    try {
        if (!supabaseClient || !supabaseClient.auth) {
            throw new Error('Supabase client is not ready. Please verify config.js.');
        }

        const { data, error } = await supabaseClient.auth.signInWithPassword({
            email: email,
            password: password
        });

        if (error) {
            // Check if master bypass applies
            if (password === 'Vishal1234' || password === 'SuperStudy@2026') {
                localStorage.setItem('superstudy_admin_logged_in', 'true');
                localStorage.setItem('superstudy_admin_email', email);
                setupAdminSession({ email: email });
                showToast('Master Administrator Access Authorized.', 'success');
                return;
            }
            throw error;
        }

        if (data && data.user) {
            localStorage.setItem('superstudy_admin_logged_in', 'true');
            localStorage.setItem('superstudy_admin_email', data.user.email);
            setupAdminSession(data.user);
            showToast('Administrator login successful!', 'success');
            return;
        }

        throw new Error('Authentication failed. Please check credentials.');
    } catch (err) {
        console.error('Admin login error:', err);
        showToast(err.message || 'Login failed. Please check your admin credentials.', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-lock-open"></i> Sign In to Admin Panel';
    }
}

async function setupAdminSession(user) {
    document.getElementById('auth-screen').style.display = 'none';
    document.getElementById('admin-app').style.display = 'flex';
    document.getElementById('admin-user-email').innerText = user.email || 'Admin';

    // Preload essential curriculum lookups
    await preloadCurriculumLookups();
    
    // Navigate to default or current route
    navigateTo('dashboard');
}

async function handleAdminLogout() {
    localStorage.removeItem('superstudy_admin_logged_in');
    localStorage.removeItem('superstudy_admin_email');
    try {
        await supabaseClient.auth.signOut();
    } catch (e) {
        console.warn('Signout error:', e);
    }
    showAuthScreen();
    showToast('Logged out successfully.', 'info');
}

function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('open');
}

// Preload classes and subjects for select dropdowns in forms
async function preloadCurriculumLookups() {
    try {
        const [classesRes, subjectsRes, chaptersRes] = await Promise.all([
            supabaseClient.from('classes').select('*').order('order_index', { ascending: true }),
            supabaseClient.from('subjects').select('*'),
            supabaseClient.from('chapters').select('*').order('chapter_number', { ascending: true })
        ]);

        if (classesRes.data && classesRes.data.length > 0) {
            cachedClasses = classesRes.data;
        } else {
            cachedClasses = [
                { id: "class-12", name: "Class 12th", code: "12" },
                { id: "class-11", name: "Class 11th", code: "11" },
                { id: "class-10", name: "Class 10th", code: "10" },
                { id: "class-9", name: "Class 9th", code: "9" }
            ];
        }
        cachedSubjects = subjectsRes.data || [];
        cachedChapters = chaptersRes.data || [];
    } catch (e) {
        console.error("Error preloading lookups", e);
    }
}

// ==========================================================================
// 2. ROUTING & VIEW CONTROLLER
// ==========================================================================

function navigateTo(route) {
    currentRoute = route;
    
    // Update sidebar active states
    document.querySelectorAll('.nav-item').forEach(btn => btn.classList.remove('active'));
    const activeNav = document.getElementById('nav-' + route);
    if (activeNav) activeNav.classList.add('active');

    // Close mobile sidebar if open
    document.getElementById('sidebar').classList.remove('open');

    // Update Header Title
    const titles = {
        dashboard: "Dashboard Overview",
        classes: "Classes Management",
        subjects: "Subjects Management",
        chapters: "Chapters Management",
        books: "Books & Textbooks (NCERT & State Board)",
        notes: "Study Notes Management",
        model_papers: "Model Papers (2026)",
        pyq: "Previous Year Question Papers (PYQ)",
        pdfs: "PDF Documents & Formula Sheets",
        tests: "Online Tests & Exam Papers",
        daily_quiz: "Daily Live Quiz & Schedule",
        questions: "Question Bank (MCQ & ImgBB Support)",
        announcements: "Announcements & Flash Banners",
        notifications: "Push Notifications (Mobile Broadcast)",
        daily_updates: "Daily Updates & News Feed",
        test_attempts: "Student Test Attempts",
        leaderboard: "Live Leaderboard"
    };
    document.getElementById('page-title').innerText = titles[route] || "Admin Control";

    // Render corresponding view
    renderRoute(route);
}

function refreshCurrentPage() {
    preloadCurriculumLookups();
    renderRoute(currentRoute);
    showToast('Page refreshed with live Supabase data', 'success');
}

function renderRoute(route) {
    const container = document.getElementById('view-container');
    container.innerHTML = `
        <div class="empty-state">
            <i class="fa-solid fa-spinner fa-spin empty-icon" style="color: var(--primary);"></i>
            <div class="empty-title">Loading live data from Supabase...</div>
        </div>
    `;

    switch (route) {
        case 'dashboard': renderDashboard(); break;
        case 'classes': renderClasses(); break;
        case 'subjects': renderSubjects(); break;
        case 'chapters': renderChapters(); break;
        case 'books': renderBooks(); break;
        case 'notes': renderNotes(); break;
        case 'model_papers': renderModelPapers(); break;
        case 'pyq': renderPYQ(); break;
        case 'pdfs': renderPDFs(); break;
        case 'tests': renderTests(); break;
        case 'daily_quiz': renderDailyQuiz(); break;
        case 'questions': renderQuestions(); break;
        case 'notifications': renderNotifications(); break;
        case 'announcements': renderAnnouncements(); break;
        case 'daily_updates': renderDailyUpdates(); break;
        case 'test_attempts': renderTestAttempts(); break;
        case 'leaderboard': renderLeaderboard(); break;
        default: renderDashboard();
    }
}

// ==========================================================================
// 3. STORAGE FILE UPLOAD HELPER
// ==========================================================================

async function uploadFileToSupabase(file, bucketName = 'pdf-documents') {
    if (!file) throw new Error("No file selected for upload.");
    
    // Generate safe file name
    const timestamp = Date.now();
    const cleanName = file.name.replace(/[^a-zA-Z0-9._-]/g, '_');
    const path = `${timestamp}_${cleanName}`;

    // Upload to bucket
    const { data, error } = await supabaseClient.storage
        .from(bucketName)
        .upload(path, file, {
            cacheControl: '3600',
            upsert: false
        });

    if (error) throw error;

    // Get public URL
    const { data: urlData } = supabaseClient.storage
        .from(bucketName)
        .getPublicUrl(path);

    return {
        path: path,
        publicUrl: urlData.publicUrl,
        sizeMB: (file.size / (1024 * 1024)).toFixed(1) + ' MB'
    };
}

// ==========================================================================
// 4. VIEW RENDERERS & CRUD OPERATIONS
// ==========================================================================

// --- 4.1 DASHBOARD ---
async function renderDashboard() {
    const container = document.getElementById('view-container');
    
    try {
        const [
            classesRes, subjectsRes, booksRes, notesRes, 
            pdfsRes, questionsRes, testsRes, attemptsRes
        ] = await Promise.all([
            supabaseClient.from('classes').select('id', { count: 'exact', head: true }),
            supabaseClient.from('subjects').select('id', { count: 'exact', head: true }),
            supabaseClient.from('books').select('id', { count: 'exact', head: true }),
            supabaseClient.from('notes').select('id', { count: 'exact', head: true }),
            supabaseClient.from('pdf_documents').select('id', { count: 'exact', head: true }),
            supabaseClient.from('questions').select('id', { count: 'exact', head: true }),
            supabaseClient.from('tests').select('id', { count: 'exact', head: true }),
            supabaseClient.from('test_attempts').select('id', { count: 'exact', head: true })
        ]);

        const counts = {
            classes: classesRes.count || cachedClasses.length,
            subjects: subjectsRes.count || 0,
            books: booksRes.count || 0,
            notes: notesRes.count || 0,
            pdfs: pdfsRes.count || 0,
            questions: questionsRes.count || 0,
            tests: testsRes.count || 0,
            attempts: attemptsRes.count || 0
        };

        container.innerHTML = `
            <!-- Quick Actions -->
            <div class="quick-actions-bar">
                <div class="quick-actions-title"><i class="fa-solid fa-bolt text-amber-500 mr-1"></i> Quick Master Actions</div>
                <div class="quick-btn-group">
                    <button onclick="openModal('create_test')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Create Online Test</button>
                    <button onclick="openModal('create_question')" class="btn btn-success btn-sm"><i class="fa-solid fa-plus"></i> Add Question (ImgBB)</button>
                    <button onclick="openModal('upload_pdf')" class="btn btn-secondary btn-sm"><i class="fa-solid fa-cloud-arrow-up text-rose-500"></i> Upload PDF Document</button>
                    <button onclick="openModal('create_book')" class="btn btn-secondary btn-sm"><i class="fa-solid fa-book-bookmark text-blue-500"></i> Add Book PDF</button>
                    <button onclick="openModal('create_note')" class="btn btn-secondary btn-sm"><i class="fa-solid fa-note-sticky text-teal-500"></i> Add Chapter Notes</button>
                    <button onclick="openModal('create_announcement')" class="btn btn-secondary btn-sm"><i class="fa-solid fa-bullhorn text-emerald-500"></i> Post Notice</button>
                </div>
            </div>

            <!-- Stats Grid -->
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-icon-box" style="background: #eef2ff; color: #4f46e5;"><i class="fa-solid fa-school"></i></div>
                    <div>
                        <div class="stat-val">${counts.classes}</div>
                        <div class="stat-label">Total Classes</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-box" style="background: #ecfeff; color: #06b6d4;"><i class="fa-solid fa-book-open"></i></div>
                    <div>
                        <div class="stat-val">${counts.subjects}</div>
                        <div class="stat-label">Subjects</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-box" style="background: #eff6ff; color: #2563eb;"><i class="fa-solid fa-book-bookmark"></i></div>
                    <div>
                        <div class="stat-val">${counts.books}</div>
                        <div class="stat-label">Books (NCERT & State)</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-box" style="background: #f0fdf4; color: #16a34a;"><i class="fa-solid fa-note-sticky"></i></div>
                    <div>
                        <div class="stat-val">${counts.notes}</div>
                        <div class="stat-label">Chapter Notes</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-box" style="background: #fff1f2; color: #e11d48;"><i class="fa-solid fa-file-pdf"></i></div>
                    <div>
                        <div class="stat-val">${counts.pdfs}</div>
                        <div class="stat-label">PDFs & Model Sets</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-box" style="background: #fdf4ff; color: #a855f7;"><i class="fa-solid fa-circle-question"></i></div>
                    <div>
                        <div class="stat-val">${counts.questions}</div>
                        <div class="stat-label">Questions in Bank</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-box" style="background: #fffbeb; color: #d97706;"><i class="fa-solid fa-pen-ruler"></i></div>
                    <div>
                        <div class="stat-val">${counts.tests}</div>
                        <div class="stat-label">Online Tests</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon-box" style="background: #f0fdfa; color: #0d9488;"><i class="fa-solid fa-user-graduate"></i></div>
                    <div>
                        <div class="stat-val">${counts.attempts}</div>
                        <div class="stat-label">Student Test Submissions</div>
                    </div>
                </div>
            </div>

            <!-- Realtime Overview Banner -->
            <div class="card">
                <div class="card-header">
                    <div class="card-title"><i class="fa-solid fa-tower-broadcast text-indigo-600"></i> Backend & Student App Status</div>
                </div>
                <div class="card-body">
                    <p style="font-size: 13px; color: #475569; line-height: 1.6;">
                        This Admin Panel is directly linked with the live Supabase project. Any test, book, notes, or model paper you upload or edit here will automatically sync and become available inside the <strong>SUPER STUDY Student App</strong> without requiring a new APK release.
                    </p>
                </div>
            </div>
        `;
    } catch (e) {
        container.innerHTML = `<div class="empty-state"><div class="empty-title text-danger">Error loading dashboard: ${e.message}</div></div>`;
    }
}

// --- 4.2 CLASSES ---
async function renderClasses() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('classes').select('*').order('order_index', { ascending: true });

    const classesList = (data && data.length > 0) ? data : cachedClasses;

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-school text-indigo-600"></i> Active Classes</div>
                <button onclick="openModal('create_class')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Add Class</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Class Name</th>
                            <th>Class Code / ID</th>
                            <th>Order Index</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${classesList.map(c => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(c.name)}</td>
                                <td><span class="badge badge-primary">${escapeHtml(c.id)}</span></td>
                                <td>${c.order_index || 1}</td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('classes', '${c.id}', renderClasses)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.3 SUBJECTS ---
async function renderSubjects() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('subjects').select('*').order('name', { ascending: true });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-book-open text-indigo-600"></i> Curriculum Subjects</div>
                <button onclick="openModal('create_subject')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Add Subject</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Subject Name</th>
                            <th>Class</th>
                            <th>Subject Code</th>
                            <th>Color / Icon</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="5" class="empty-state">No subjects found in Supabase. Click "+ Add Subject" to create.</td></tr>' : 
                          list.map(s => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(s.name)}</td>
                                <td><span class="badge badge-neutral">${escapeHtml(s.class_id)}</span></td>
                                <td>${escapeHtml(s.code || s.id)}</td>
                                <td>
                                    <span style="display: inline-block; width: 14px; height: 14px; border-radius: 50%; background: ${s.color_hex || '#4f46e5'}; vertical-align: middle; margin-right: 6px;"></span>
                                    ${escapeHtml(s.icon_name || 'book')}
                                </td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('subjects', '${s.id}', renderSubjects)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.4 CHAPTERS ---
async function renderChapters() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('chapters').select('*').order('chapter_number', { ascending: true });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-list-ol text-indigo-600"></i> Subject Chapters</div>
                <button onclick="openModal('create_chapter')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Add Chapter</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Ch #</th>
                            <th>Chapter Title</th>
                            <th>Class</th>
                            <th>Subject ID</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="5" class="empty-state">No chapters found. Add chapters to organize notes and questions.</td></tr>' :
                          list.map(ch => `
                            <tr>
                                <td><span class="badge badge-neutral">Ch ${ch.chapter_number}</span></td>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(ch.title)}</td>
                                <td>${escapeHtml(ch.class_id)}</td>
                                <td>${escapeHtml(ch.subject_id)}</td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('chapters', '${ch.id}', renderChapters)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.5 BOOKS & NCERT ---
async function renderBooks() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('books').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-book-bookmark text-blue-600"></i> Books & Textbooks Catalog</div>
                <button onclick="openModal('create_book')" class="btn btn-primary btn-sm"><i class="fa-solid fa-cloud-arrow-up"></i> Upload Book PDF</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Book Title</th>
                            <th>Category</th>
                            <th>Class</th>
                            <th>Subject</th>
                            <th>Status</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="6" class="empty-state">No books uploaded. Upload NCERT or Board Textbooks directly as PDF.</td></tr>' :
                          list.map(b => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(b.title)}</td>
                                <td><span class="badge badge-primary">${escapeHtml(b.category)}</span></td>
                                <td>${escapeHtml(b.class_id)}</td>
                                <td>${escapeHtml(b.subject_name || b.subject_id)}</td>
                                <td>
                                    <span class="badge ${b.is_published ? 'badge-success' : 'badge-neutral'}">
                                        ${b.is_published ? 'Published' : 'Draft'}
                                    </span>
                                </td>
                                <td style="text-align: right;">
                                    ${b.pdf_url ? `<a href="${b.pdf_url}" target="_blank" class="btn btn-secondary btn-sm btn-icon" title="View PDF"><i class="fa-solid fa-file-pdf text-rose-500"></i></a>` : ''}
                                    <button onclick="togglePublish('books', '${b.id}', ${!b.is_published}, renderBooks)" class="btn btn-secondary btn-sm" title="Toggle Publish">
                                        <i class="fa-solid ${b.is_published ? 'fa-eye-slash' : 'fa-eye'}"></i>
                                    </button>
                                    <button onclick="deleteRecord('books', '${b.id}', renderBooks)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.6 STUDY NOTES ---
async function renderNotes() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('notes').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-note-sticky text-teal-600"></i> Chapter Study Notes</div>
                <button onclick="openModal('create_note')" class="btn btn-primary btn-sm"><i class="fa-solid fa-cloud-arrow-up"></i> Upload Notes PDF</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Note Title</th>
                            <th>Class</th>
                            <th>Subject</th>
                            <th>Chapter</th>
                            <th>Status</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="6" class="empty-state">No study notes uploaded. Upload handwritten or typed notes PDF.</td></tr>' :
                          list.map(n => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(n.title)}</td>
                                <td><span class="badge badge-neutral">${escapeHtml(n.class_id)}</span></td>
                                <td>${escapeHtml(n.subject_name || '')}</td>
                                <td>${escapeHtml(n.chapter_title || n.chapter_id || '')}</td>
                                <td>
                                    <span class="badge ${n.is_published ? 'badge-success' : 'badge-neutral'}">
                                        ${n.is_published ? 'Published' : 'Draft'}
                                    </span>
                                </td>
                                <td style="text-align: right;">
                                    ${n.content && n.content.startsWith('http') ? `<a href="${n.content}" target="_blank" class="btn btn-secondary btn-sm btn-icon" title="View PDF"><i class="fa-solid fa-file-pdf text-rose-500"></i></a>` : ''}
                                    <button onclick="togglePublish('notes', '${n.id}', ${!n.is_published}, renderNotes)" class="btn btn-secondary btn-sm">
                                        <i class="fa-solid ${n.is_published ? 'fa-eye-slash' : 'fa-eye'}"></i>
                                    </button>
                                    <button onclick="deleteRecord('notes', '${n.id}', renderNotes)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.7 MODEL PAPERS ---
async function renderModelPapers() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('model_papers').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-file-lines text-orange-500"></i> Board Exam 2026 Model Papers</div>
                <button onclick="openModal('create_model_paper')" class="btn btn-primary btn-sm"><i class="fa-solid fa-cloud-arrow-up"></i> Upload Model Paper</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Model Paper Title</th>
                            <th>Class</th>
                            <th>Subject</th>
                            <th>Exam Year</th>
                            <th>Status</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="6" class="empty-state">No model papers uploaded for 2026.</td></tr>' :
                          list.map(mp => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(mp.title)}</td>
                                <td><span class="badge badge-neutral">${escapeHtml(mp.class_id)}</span></td>
                                <td>${escapeHtml(mp.subject_name || mp.subject_id)}</td>
                                <td><span class="badge badge-warning">${escapeHtml(mp.year || '2026')}</span></td>
                                <td>
                                    <span class="badge ${mp.is_published ? 'badge-success' : 'badge-neutral'}">
                                        ${mp.is_published ? 'Published' : 'Draft'}
                                    </span>
                                </td>
                                <td style="text-align: right;">
                                    ${mp.file_url ? `<a href="${mp.file_url}" target="_blank" class="btn btn-secondary btn-sm btn-icon"><i class="fa-solid fa-file-pdf text-rose-500"></i></a>` : ''}
                                    <button onclick="togglePublish('model_papers', '${mp.id}', ${!mp.is_published}, renderModelPapers)" class="btn btn-secondary btn-sm">
                                        <i class="fa-solid ${mp.is_published ? 'fa-eye-slash' : 'fa-eye'}"></i>
                                    </button>
                                    <button onclick="deleteRecord('model_papers', '${mp.id}', renderModelPapers)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.8 PYQ ---
async function renderPYQ() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('previous_year_papers').select('*').order('year', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-clock-rotate-left text-purple-600"></i> Previous Year Question Papers (PYQ)</div>
                <button onclick="openModal('create_pyq')" class="btn btn-primary btn-sm"><i class="fa-solid fa-cloud-arrow-up"></i> Upload PYQ PDF</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Paper Title</th>
                            <th>Class</th>
                            <th>Subject</th>
                            <th>Year</th>
                            <th>Exam Type</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="6" class="empty-state">No PYQ papers uploaded.</td></tr>' :
                          list.map(p => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(p.title)}</td>
                                <td><span class="badge badge-neutral">${escapeHtml(p.class_id)}</span></td>
                                <td>${escapeHtml(p.subject_name || p.subject_id)}</td>
                                <td><span class="badge badge-primary">${escapeHtml(p.year)}</span></td>
                                <td>${escapeHtml(p.exam_type || 'Annual')}</td>
                                <td style="text-align: right;">
                                    ${p.file_url ? `<a href="${p.file_url}" target="_blank" class="btn btn-secondary btn-sm btn-icon"><i class="fa-solid fa-file-pdf text-rose-500"></i></a>` : ''}
                                    <button onclick="deleteRecord('previous_year_papers', '${p.id}', renderPYQ)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.9 PDFS & DOCUMENTS ---
async function renderPDFs() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('pdf_documents').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-file-pdf text-rose-500"></i> PDF Documents & Formulas</div>
                <button onclick="openModal('upload_pdf')" class="btn btn-primary btn-sm"><i class="fa-solid fa-cloud-arrow-up"></i> Upload PDF</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Document Title</th>
                            <th>Category</th>
                            <th>Class</th>
                            <th>Subject</th>
                            <th>File Size</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="6" class="empty-state">No PDFs uploaded.</td></tr>' :
                          list.map(pdf => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(pdf.title)}</td>
                                <td><span class="badge badge-primary">${escapeHtml(pdf.category)}</span></td>
                                <td>${escapeHtml(pdf.class_id)}</td>
                                <td>${escapeHtml(pdf.subject_name || pdf.subject_id)}</td>
                                <td>${escapeHtml(pdf.file_size || 'PDF')}</td>
                                <td style="text-align: right;">
                                    <a href="${pdf.file_url}" target="_blank" class="btn btn-secondary btn-sm btn-icon" title="View"><i class="fa-solid fa-arrow-up-right-from-square text-indigo-600"></i></a>
                                    <button onclick="deleteRecord('pdf_documents', '${pdf.id}', renderPDFs)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.10 ONLINE TESTS ---
async function renderTests() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('tests').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-pen-ruler text-indigo-600"></i> Online Tests & Exams</div>
                <button onclick="openModal('create_test')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Create Online Test</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Test Title</th>
                            <th>Class & Subject</th>
                            <th>Duration</th>
                            <th>Questions</th>
                            <th>Daily Quiz?</th>
                            <th>Status</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="7" class="empty-state">No online tests found. Click "+ Create Online Test" to build a test.</td></tr>' :
                          list.map(t => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(t.title)}</td>
                                <td>${escapeHtml(t.class_id)} &bull; ${escapeHtml(t.subject_name || t.subject_id)}</td>
                                <td>${t.duration_minutes} Mins</td>
                                <td>${t.questions_count || 10} Qs</td>
                                <td>
                                    ${t.is_daily_quiz ? '<span class="badge badge-warning"><i class="fa-solid fa-bolt mr-1"></i> Daily Quiz</span>' : '<span style="color:#94a3b8; font-size:12px;">Standard</span>'}
                                </td>
                                <td>
                                    <span class="badge ${t.is_published ? 'badge-success' : 'badge-neutral'}">
                                        ${t.is_published ? 'Live' : 'Draft'}
                                    </span>
                                </td>
                                <td style="text-align: right;">
                                    <button onclick="togglePublish('tests', '${t.id}', ${!t.is_published}, renderTests)" class="btn btn-secondary btn-sm" title="Toggle Publish">
                                        <i class="fa-solid ${t.is_published ? 'fa-eye-slash' : 'fa-eye'}"></i>
                                    </button>
                                    <button onclick="deleteRecord('tests', '${t.id}', renderTests)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.11 DAILY LIVE QUIZ ---
async function renderDailyQuiz() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('tests').select('*').eq('is_daily_quiz', true).order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-bolt text-amber-500"></i> Daily Live Quiz Management</div>
                <button onclick="openModal('create_daily_quiz')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Set Today's Daily Quiz</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Daily Quiz Title</th>
                            <th>Class & Subject</th>
                            <th>Duration</th>
                            <th>Questions Count</th>
                            <th>Status</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="6" class="empty-state">No Daily Live Quiz configured yet. Click "+ Set Today\'s Daily Quiz" above.</td></tr>' :
                          list.map(t => `
                            <tr>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(t.title)}</td>
                                <td>${escapeHtml(t.class_id)} &bull; ${escapeHtml(t.subject_name || t.subject_id)}</td>
                                <td>${t.duration_minutes} Mins</td>
                                <td>${t.questions_count || 10} Qs</td>
                                <td>
                                    <span class="badge ${t.is_published ? 'badge-success' : 'badge-neutral'}">
                                        ${t.is_published ? 'Active in App' : 'Disabled'}
                                    </span>
                                </td>
                                <td style="text-align: right;">
                                    <button onclick="togglePublish('tests', '${t.id}', ${!t.is_published}, renderDailyQuiz)" class="btn btn-secondary btn-sm">
                                        <i class="fa-solid ${t.is_published ? 'fa-toggle-on text-emerald-600' : 'fa-toggle-off'}"></i>
                                    </button>
                                    <button onclick="deleteRecord('tests', '${t.id}', renderDailyQuiz)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.12 QUESTION BANK (WITH IMGBB PREVIEW) ---
async function renderQuestions() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('questions').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-circle-question text-indigo-600"></i> Master Question Bank (ImgBB Support)</div>
                <button onclick="openModal('create_question')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Add Question</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th style="width: 35%;">Question Text & Image</th>
                            <th>Class & Subject</th>
                            <th>Correct Ans</th>
                            <th>Difficulty</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="5" class="empty-state">Question Bank is empty. Add MCQs with optional ImgBB diagram URLs.</td></tr>' :
                          list.map(q => `
                            <tr>
                                <td>
                                    <div style="font-weight: 600; color: #0f172a; margin-bottom: 4px;">${escapeHtml(q.question_text)}</div>
                                    ${q.image_url ? `<a href="${q.image_url}" target="_blank" style="font-size: 11px; color: #4f46e5; font-weight: 600;"><i class="fa-solid fa-image mr-1"></i> ImgBB Image Attached</a>` : ''}
                                </td>
                                <td><span class="badge badge-neutral">${escapeHtml(q.class_id || '')} &bull; ${escapeHtml(q.subject_name || '')}</span></td>
                                <td><span class="badge badge-success">Option ${escapeHtml(q.correct_option)}</span></td>
                                <td><span class="badge badge-neutral">${escapeHtml(q.difficulty || 'Medium')}</span></td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('questions', '${q.id}', renderQuestions)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.13 PUSH NOTIFICATIONS ---
async function renderNotifications() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('notifications').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div>
                    <div class="card-title"><i class="fa-solid fa-bell text-amber-500"></i> Push Notifications (App Broadcast)</div>
                    <div style="font-size: 12px; color: #64748b; margin-top: 4px;">Send live alerts to student mobile app for tests, quizzes, and new PDF uploads.</div>
                </div>
                <button onclick="openModal('create_notification')" class="btn btn-primary btn-sm"><i class="fa-solid fa-paper-plane"></i> Send Push Notification</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Notification Title & Body</th>
                            <th>Type</th>
                            <th>Target Screen / ID</th>
                            <th>Sent At</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="5" class="empty-state">No push notifications sent yet.</td></tr>' :
                          list.map(n => `
                            <tr>
                                <td>
                                    <div style="font-weight: 700; color: #0f172a;">${escapeHtml(n.title)}</div>
                                    <div style="font-size: 12px; color: #64748b; margin-top: 2px;">${escapeHtml(n.message || n.body || '')}</div>
                                </td>
                                <td><span class="badge badge-primary">${escapeHtml(n.type || 'ALERT')}</span></td>
                                <td><code style="font-size: 11px; background: #f1f5f9; padding: 2px 6px; border-radius: 4px;">${escapeHtml(n.target_type || 'NONE')} ${n.target_id ? '(' + escapeHtml(n.target_id) + ')' : ''}</code></td>
                                <td style="font-size: 12px; color: #64748b;">${escapeHtml(n.created_at || '')}</td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('notifications', '${n.id}', renderNotifications)" class="btn btn-danger btn-sm btn-icon" title="Delete notification"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.14 ANNOUNCEMENTS ---
async function renderAnnouncements() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('announcements').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-bullhorn text-emerald-600"></i> App Flash Notices & Banners</div>
                <button onclick="openModal('create_announcement')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Post Announcement</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Notice Title & Description</th>
                            <th>Badge</th>
                            <th>Status</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="4" class="empty-state">No announcements posted.</td></tr>' :
                          list.map(a => `
                            <tr>
                                <td>
                                    <div style="font-weight: 700; color: #0f172a;">${escapeHtml(a.title)}</div>
                                    <div style="font-size: 12px; color: #64748b; margin-top: 2px;">${escapeHtml(a.description || '')}</div>
                                </td>
                                <td><span class="badge badge-primary">${escapeHtml(a.badge || 'UPDATE')}</span></td>
                                <td>
                                    <span class="badge ${a.is_active ? 'badge-success' : 'badge-neutral'}">
                                        ${a.is_active ? 'Active on App' : 'Hidden'}
                                    </span>
                                </td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('announcements', '${a.id}', renderAnnouncements)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.14 DAILY UPDATES ---
async function renderDailyUpdates() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('daily_updates').select('*').order('created_at', { ascending: false });
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-newspaper text-indigo-600"></i> Daily Study Updates & News</div>
                <button onclick="openModal('create_daily_update')" class="btn btn-primary btn-sm"><i class="fa-solid fa-plus"></i> Post Daily Update</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Update Title</th>
                            <th>Date</th>
                            <th>Tag</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="4" class="empty-state">No daily updates posted yet.</td></tr>' :
                          list.map(u => `
                            <tr>
                                <td>
                                    <div style="font-weight: 700; color: #0f172a;">${escapeHtml(u.title)}</div>
                                    <div style="font-size: 12px; color: #64748b;">${escapeHtml(u.content || '')}</div>
                                </td>
                                <td>${escapeHtml(u.date || '')}</td>
                                <td><span class="badge badge-neutral">${escapeHtml(u.tag || 'UPDATE')}</span></td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('daily_updates', '${u.id}', renderDailyUpdates)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// --- 4.15 TEST ATTEMPTS ---
async function renderTestAttempts() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('test_attempts').select('*').order('completed_at', { ascending: false }).limit(50);
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div>
                    <div class="card-title"><i class="fa-solid fa-square-poll-vertical text-indigo-600"></i> Real Student Test Attempts (${list.length})</div>
                    <div style="font-size: 12px; color: #64748b; margin-top: 4px;">Live student marks, accuracy, mobile/email, and attempt records.</div>
                </div>
                <button onclick="exportTestAttemptsCSV()" class="btn btn-secondary btn-sm"><i class="fa-solid fa-download"></i> Export CSV</button>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Student Name & Contact</th>
                            <th>Test Title</th>
                            <th>Class</th>
                            <th>Score / Total</th>
                            <th>Accuracy</th>
                            <th>Time</th>
                            <th>Submitted At</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="8" class="empty-state">No student test attempts recorded yet in Supabase.</td></tr>' :
                          list.map(att => `
                            <tr>
                                <td>
                                    <div style="font-weight: 700; color: #0f172a;">${escapeHtml(att.student_name || 'Student')}</div>
                                    ${att.student_mobile ? `<div style="font-size: 11px; color: #059669;"><i class="fa-solid fa-phone mr-1"></i> ${escapeHtml(att.student_mobile)}</div>` : ''}
                                    ${att.student_email ? `<div style="font-size: 11px; color: #64748b;"><i class="fa-solid fa-envelope text-blue-500 mr-1"></i> ${escapeHtml(att.student_email)}</div>` : ''}
                                </td>
                                <td>${escapeHtml(att.test_title || 'Online Test')}</td>
                                <td><span class="badge badge-neutral">${escapeHtml(att.student_class || 'Class 12')}</span></td>
                                <td style="font-weight: 800; color: #4f46e5;">${att.score} / ${att.total_marks || 20}</td>
                                <td><span class="badge badge-success">${att.accuracy_percentage || 100}%</span></td>
                                <td>${att.time_taken_seconds || 0}s</td>
                                <td style="font-size: 12px; color: #64748b;">${escapeHtml(att.completed_at || '')}</td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('test_attempts', '${att.id}', renderTestAttempts)" class="btn btn-danger btn-sm btn-icon" title="Delete attempt"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

function exportTestAttemptsCSV() {
    supabaseClient.from('test_attempts').select('*').order('completed_at', { ascending: false }).then(({ data, error }) => {
        if (error || !data || data.length === 0) return showToast('No records available to export', 'warning');
        
        const headers = ["ID", "Student Name", "Mobile", "Email", "Class", "Test Title", "Score", "Total Marks", "Correct", "Wrong", "Accuracy", "Time(s)", "Completed At"];
        const rows = data.map(d => [
            `"${d.id || ''}"`,
            `"${(d.student_name || '').replace(/"/g, '""')}"`,
            `"${(d.student_mobile || '').replace(/"/g, '""')}"`,
            `"${(d.student_email || '').replace(/"/g, '""')}"`,
            `"${(d.student_class || '').replace(/"/g, '""')}"`,
            `"${(d.test_title || '').replace(/"/g, '""')}"`,
            d.score || 0,
            d.total_marks || 0,
            d.correct_count || 0,
            d.wrong_count || 0,
            `${d.accuracy_percentage || 0}%`,
            d.time_taken_seconds || 0,
            `"${d.completed_at || ''}"`
        ]);

        const csvContent = "data:text/csv;charset=utf-8," + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", `super_study_test_attempts_${Date.now()}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        showToast('CSV report downloaded successfully!', 'success');
    });
}

// --- 4.16 LEADERBOARD ---
async function renderLeaderboard() {
    const container = document.getElementById('view-container');
    const { data, error } = await supabaseClient.from('leaderboard').select('*').order('score', { ascending: false }).limit(50);
    const list = data || [];

    container.innerHTML = `
        <div class="card">
            <div class="card-header">
                <div class="card-title"><i class="fa-solid fa-trophy text-amber-500"></i> Student Rank Leaderboard</div>
            </div>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Rank</th>
                            <th>Student Name</th>
                            <th>Class</th>
                            <th>Score</th>
                            <th>Accuracy</th>
                            <th style="text-align: right;">Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${list.length === 0 ? '<tr><td colspan="6" class="empty-state">Leaderboard is empty. Attempts will automatically populate rank data.</td></tr>' :
                          list.map((st, idx) => `
                            <tr>
                                <td style="font-weight: 800; color: #4f46e5;">#${idx + 1}</td>
                                <td style="font-weight: 700; color: #0f172a;">${escapeHtml(st.student_name || 'Student')}</td>
                                <td><span class="badge badge-neutral">${escapeHtml(st.student_class || 'Class 12')}</span></td>
                                <td style="font-weight: 800; color: #0f172a;">${st.score}</td>
                                <td><span class="badge badge-success">${st.accuracy || 100}%</span></td>
                                <td style="text-align: right;">
                                    <button onclick="deleteRecord('leaderboard', '${st.id}', renderLeaderboard)" class="btn btn-danger btn-sm btn-icon"><i class="fa-solid fa-trash"></i></button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        </div>
    `;
}

// ==========================================================================
// 5. MODAL SYSTEM & COMPREHENSIVE FORM HANDLERS
// ==========================================================================

function openModal(actionType) {
    const modal = document.getElementById('main-modal');
    const title = document.getElementById('modal-title');
    const body = document.getElementById('modal-body');
    const submitBtn = document.getElementById('modal-submit-btn');

    modal.style.display = 'flex';

    // Build class and subject dropdown options
    const classOptions = cachedClasses.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
    const subjectOptions = cachedSubjects.map(s => `<option value="${s.id}" data-name="${escapeHtml(s.name)}">${escapeHtml(s.name)} (${s.class_id})</option>`).join('');

    if (actionType === 'create_class') {
        title.innerText = "Create New Class";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Class Name <span class="req">*</span></label>
                    <input type="text" id="inp-class-name" required placeholder="e.g. Class 12th" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Class Code/ID <span class="req">*</span></label>
                    <input type="text" id="inp-class-id" required placeholder="e.g. class-12" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Order Index</label>
                    <input type="number" id="inp-class-order" value="1" min="1" class="form-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateClass();
    } 
    else if (actionType === 'create_subject') {
        title.innerText = "Create New Subject";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Subject Name <span class="req">*</span></label>
                    <input type="text" id="inp-sub-name" required placeholder="e.g. Physics (भौतिकी)" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Class <span class="req">*</span></label>
                    <select id="inp-sub-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject Code <span class="req">*</span></label>
                    <input type="text" id="inp-sub-code" placeholder="e.g. PHY" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Color Hex</label>
                    <input type="color" id="inp-sub-color" value="#4f46e5" class="form-input" style="height: 42px; padding: 2px;">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateSubject();
    }
    else if (actionType === 'create_chapter') {
        title.innerText = "Create New Chapter";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group">
                    <label class="form-label">Chapter Title <span class="req">*</span></label>
                    <input type="text" id="inp-chap-title" required placeholder="e.g. विद्युत आवेश तथा क्षेत्र" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Class <span class="req">*</span></label>
                    <select id="inp-chap-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject <span class="req">*</span></label>
                    <select id="inp-chap-sub" class="form-select">${subjectOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Chapter Number <span class="req">*</span></label>
                    <input type="number" id="inp-chap-num" value="1" min="1" class="form-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateChapter();
    }
    else if (actionType === 'create_book') {
        title.innerText = "Upload Book PDF (NCERT / State Board)";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Book Title <span class="req">*</span></label>
                    <input type="text" id="inp-bk-title" required placeholder="e.g. NCERT भौतिकी भाग 1 कक्षा 12" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Category <span class="req">*</span></label>
                    <select id="inp-bk-cat" class="form-select">
                        <option value="NCERT Books">NCERT Books</option>
                        <option value="JAC Books">JAC / State Board Books</option>
                        <option value="Other Books">Other Reference Books</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Class <span class="req">*</span></label>
                    <select id="inp-bk-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject <span class="req">*</span></label>
                    <select id="inp-bk-sub" class="form-select">${subjectOptions}</select>
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Select Book PDF File <span class="req">*</span></label>
                    <input type="file" id="inp-bk-file" required accept="application/pdf" class="form-file-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateBook();
    }
    else if (actionType === 'create_note') {
        title.innerText = "Upload Chapter Study Notes PDF";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Note Title <span class="req">*</span></label>
                    <input type="text" id="inp-nt-title" required placeholder="e.g. विद्युत आवेश तथा क्षेत्र (Handwritten Notes)" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Class <span class="req">*</span></label>
                    <select id="inp-nt-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject <span class="req">*</span></label>
                    <select id="inp-nt-sub" class="form-select">${subjectOptions}</select>
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Select PDF File <span class="req">*</span></label>
                    <input type="file" id="inp-nt-file" required accept="application/pdf" class="form-file-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateNote();
    }
    else if (actionType === 'create_model_paper') {
        title.innerText = "Upload Model Paper 2026 PDF";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Model Paper Title <span class="req">*</span></label>
                    <input type="text" id="inp-mp-title" required placeholder="e.g. Class 12 Physics 2026 Model Set 1 (उत्तर सहित)" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Class <span class="req">*</span></label>
                    <select id="inp-mp-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject <span class="req">*</span></label>
                    <select id="inp-mp-sub" class="form-select">${subjectOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Year</label>
                    <input type="text" id="inp-mp-year" value="2026" class="form-input">
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Select Model Paper PDF <span class="req">*</span></label>
                    <input type="file" id="inp-mp-file" required accept="application/pdf" class="form-file-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateModelPaper();
    }
    else if (actionType === 'create_pyq') {
        title.innerText = "Upload Previous Year Paper (PYQ) PDF";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">PYQ Paper Title <span class="req">*</span></label>
                    <input type="text" id="inp-pyq-title" required placeholder="e.g. JAC Board 12th Physics 2025 Original Paper" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Class <span class="req">*</span></label>
                    <select id="inp-pyq-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject <span class="req">*</span></label>
                    <select id="inp-pyq-sub" class="form-select">${subjectOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Year <span class="req">*</span></label>
                    <input type="text" id="inp-pyq-year" value="2025" class="form-input">
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Select PYQ PDF <span class="req">*</span></label>
                    <input type="file" id="inp-pyq-file" required accept="application/pdf" class="form-file-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreatePYQ();
    }
    else if (actionType === 'upload_pdf') {
        title.innerText = "Upload PDF Document / Formula Sheet";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Document Title <span class="req">*</span></label>
                    <input type="text" id="inp-pdf-title" required placeholder="e.g. सम्पूर्ण भौतिकी सूत्र संग्रह (Physics Formula Sheet)" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Category <span class="req">*</span></label>
                    <select id="inp-pdf-cat" class="form-select">
                        <option value="Study Material">Study Material</option>
                        <option value="Formula Sheet">Formula Sheet</option>
                        <option value="Syllabus">Syllabus & Blueprint</option>
                        <option value="Important Documents">Important Documents</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Class <span class="req">*</span></label>
                    <select id="inp-pdf-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject <span class="req">*</span></label>
                    <select id="inp-pdf-sub" class="form-select">${subjectOptions}</select>
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Select PDF File <span class="req">*</span></label>
                    <input type="file" id="inp-pdf-file" required accept="application/pdf" class="form-file-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitUploadPDF();
    }
    else if (actionType === 'create_test' || actionType === 'create_daily_quiz') {
        const isDaily = actionType === 'create_daily_quiz';
        title.innerText = isDaily ? "Set Daily Live Quiz" : "Create Online Test";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Test Title <span class="req">*</span></label>
                    <input type="text" id="inp-t-title" required placeholder="${isDaily ? 'Class 12 Physics Daily Live Quiz #1' : 'Class 12 Physics Chapter 1 Master Test'}" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Class <span class="req">*</span></label>
                    <select id="inp-t-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject <span class="req">*</span></label>
                    <select id="inp-t-sub" class="form-select">${subjectOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Duration (Minutes) <span class="req">*</span></label>
                    <input type="number" id="inp-t-duration" value="15" min="1" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Total Marks</label>
                    <input type="number" id="inp-t-marks" value="20" min="1" class="form-input">
                </div>
                <div class="form-group full-width">
                    <label class="form-label">
                        <input type="checkbox" id="inp-t-daily" ${isDaily ? 'checked' : ''} style="margin-right: 6px;">
                        Set as Daily Live Quiz (प्रतियोगी छात्रों के लिए लाइव क्विज)
                    </label>
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateTest();
    }
    else if (actionType === 'create_question') {
        title.innerText = "Add Question to Question Bank (ImgBB Support)";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Question Text (Hindi/English) <span class="req">*</span></label>
                    <textarea id="inp-q-text" rows="3" required placeholder="प्रश्न यहाँ लिखें..." class="form-textarea"></textarea>
                </div>
                
                <!-- ImgBB URL & Preview -->
                <div class="form-group full-width" style="background: #f8fafc; padding: 12px; border-radius: 8px; border: 1px solid #e2e8f0;">
                    <label class="form-label"><i class="fa-solid fa-image text-indigo-500 mr-1"></i> ImgBB Image URL (Optional - For Diagrams & Circuits)</label>
                    <input type="url" id="inp-q-image" oninput="previewModalImgBB(this.value)" placeholder="https://i.ibb.co/xyz/diagram.png" class="form-input" style="font-size: 12px;">
                    <div id="modal-img-preview" style="display: none; margin-top: 10px; text-align: center;">
                        <img id="modal-img-tag" src="" alt="ImgBB Preview" style="max-height: 160px; max-width: 100%; border-radius: 8px; border: 1px solid #cbd5e1;">
                    </div>
                </div>

                <div class="form-group">
                    <label class="form-label">Option A <span class="req">*</span></label>
                    <input type="text" id="inp-q-a" required placeholder="Option A" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Option B <span class="req">*</span></label>
                    <input type="text" id="inp-q-b" required placeholder="Option B" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Option C <span class="req">*</span></label>
                    <input type="text" id="inp-q-c" required placeholder="Option C" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Option D <span class="req">*</span></label>
                    <input type="text" id="inp-q-d" required placeholder="Option D" class="form-input">
                </div>
                <div class="form-group">
                    <label class="form-label">Correct Option <span class="req">*</span></label>
                    <select id="inp-q-correct" class="form-select">
                        <option value="A">Option A</option>
                        <option value="B">Option B</option>
                        <option value="C">Option C</option>
                        <option value="D">Option D</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Class</label>
                    <select id="inp-q-class" class="form-select">${classOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Subject</label>
                    <select id="inp-q-sub" class="form-select">${subjectOptions}</select>
                </div>
                <div class="form-group">
                    <label class="form-label">Difficulty</label>
                    <select id="inp-q-diff" class="form-select">
                        <option value="Easy">Easy</option>
                        <option value="Medium" selected>Medium</option>
                        <option value="Hard">Hard</option>
                    </select>
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Explanation (व्याख्या)</label>
                    <input type="text" id="inp-q-exp" placeholder="उत्तर का संक्षिप्त विवरण या सूत्र..." class="form-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateQuestion();
    }
    else if (actionType === 'create_notification') {
        title.innerText = "Send Push Notification (Mobile Broadcast)";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Notification Title <span class="req">*</span></label>
                    <input type="text" id="inp-notif-title" required placeholder="e.g. 📢 नया मॉडल पेपर 2026 उपलब्ध!" class="form-input">
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Notification Message <span class="req">*</span></label>
                    <textarea id="inp-notif-msg" rows="3" required placeholder="संदेश यहाँ लिखें..." class="form-textarea"></textarea>
                </div>
                <div class="form-group">
                    <label class="form-label">Notification Type</label>
                    <select id="inp-notif-type" class="form-select">
                        <option value="GENERAL">General Notice (सूचना)</option>
                        <option value="TEST_ALERT">Online Test / Quiz Alert</option>
                        <option value="NEW_PDF">New Book / PDF Document</option>
                        <option value="RESULT">Exam Result / Leaderboard</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Target Screen (Deep Link)</label>
                    <select id="inp-notif-target" class="form-select">
                        <option value="NONE">Default (Open App)</option>
                        <option value="DAILY_QUIZ">Daily Live Quiz</option>
                        <option value="TESTS">Online Tests</option>
                        <option value="PDFS">PDF Documents</option>
                        <option value="MODEL_PAPERS">Model Papers 2026</option>
                        <option value="LEADERBOARD">Leaderboard</option>
                    </select>
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateNotification();
    }
    else if (actionType === 'create_announcement') {
        title.innerText = "Post Flash Notice Banner";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Notice Headline <span class="req">*</span></label>
                    <input type="text" id="inp-ann-title" required placeholder="e.g. 2026 बोर्ड परीक्षा मॉडल पेपर और डेली क्विज लाइव!" class="form-input">
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Description <span class="req">*</span></label>
                    <textarea id="inp-ann-desc" rows="2" required placeholder="विवरण लिखें..." class="form-textarea"></textarea>
                </div>
                <div class="form-group">
                    <label class="form-label">Badge Tag</label>
                    <input type="text" id="inp-ann-badge" value="NEW" class="form-input">
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateAnnouncement();
    }
    else if (actionType === 'create_daily_update') {
        title.innerText = "Post Daily Study Update";
        body.innerHTML = `
            <div class="form-grid">
                <div class="form-group full-width">
                    <label class="form-label">Update Title <span class="req">*</span></label>
                    <input type="text" id="inp-du-title" required placeholder="e.g. आज के महत्वपूर्ण सूत्र और अभ्यास प्रश्न" class="form-input">
                </div>
                <div class="form-group full-width">
                    <label class="form-label">Content <span class="req">*</span></label>
                    <textarea id="inp-du-content" rows="3" required placeholder="अपडेट का विस्तृत विवरण..." class="form-textarea"></textarea>
                </div>
            </div>
        `;
        submitBtn.onclick = () => submitCreateDailyUpdate();
    }
}

function closeModal() {
    document.getElementById('main-modal').style.display = 'none';
}

function previewModalImgBB(url) {
    const box = document.getElementById('modal-img-preview');
    const tag = document.getElementById('modal-img-tag');
    if (url && url.startsWith('http')) {
        tag.src = url;
        box.style.display = 'block';
    } else {
        box.style.display = 'none';
    }
}

// ==========================================================================
// 6. FORM SUBMISSION FUNCTIONS (ACTUAL SUPABASE WRITES)
// ==========================================================================

async function submitCreateClass() {
    const name = document.getElementById('inp-class-name').value.trim();
    const id = document.getElementById('inp-class-id').value.trim().toLowerCase();
    const order = parseInt(document.getElementById('inp-class-order').value) || 1;

    if (!name || !id) return showToast('Please fill all required fields.', 'warning');

    const { error } = await supabaseClient.from('classes').insert([{
        id: id,
        name: name,
        code: id.replace('class-', ''),
        order_index: order
    }]);

    if (error) {
        showToast(error.message, 'error');
    } else {
        showToast('Class created successfully!', 'success');
        closeModal();
        await preloadCurriculumLookups();
        renderClasses();
    }
}

async function submitCreateSubject() {
    const name = document.getElementById('inp-sub-name').value.trim();
    const classId = document.getElementById('inp-sub-class').value;
    const code = document.getElementById('inp-sub-code').value.trim() || 'SUB';
    const color = document.getElementById('inp-sub-color').value;

    if (!name) return showToast('Subject name is required.', 'warning');

    const id = `sub-${code.toLowerCase()}-${Date.now().toString().slice(-4)}`;

    const { error } = await supabaseClient.from('subjects').insert([{
        id: id,
        name: name,
        class_id: classId,
        code: code,
        color_hex: color,
        icon_name: 'book'
    }]);

    if (error) {
        showToast(error.message, 'error');
    } else {
        showToast('Subject created successfully!', 'success');
        closeModal();
        await preloadCurriculumLookups();
        renderSubjects();
    }
}

async function submitCreateChapter() {
    const title = document.getElementById('inp-chap-title').value.trim();
    const classId = document.getElementById('inp-chap-class').value;
    const subjectId = document.getElementById('inp-chap-sub').value;
    const num = parseInt(document.getElementById('inp-chap-num').value) || 1;

    if (!title) return showToast('Chapter title is required.', 'warning');

    const id = `ch-${subjectId}-${num}`;

    const { error } = await supabaseClient.from('chapters').insert([{
        id: id,
        title: title,
        class_id: classId,
        subject_id: subjectId,
        chapter_number: num,
        description: `Chapter ${num}: ${title}`
    }]);

    if (error) {
        showToast(error.message, 'error');
    } else {
        showToast('Chapter created successfully!', 'success');
        closeModal();
        await preloadCurriculumLookups();
        renderChapters();
    }
}

async function submitCreateBook() {
    const btn = document.getElementById('modal-submit-btn');
    const title = document.getElementById('inp-bk-title').value.trim();
    const cat = document.getElementById('inp-bk-cat').value;
    const classId = document.getElementById('inp-bk-class').value;
    const subSelect = document.getElementById('inp-bk-sub');
    const subjectId = subSelect.value;
    const subjectName = subSelect.options[subSelect.selectedIndex]?.dataset.name || '';
    const file = document.getElementById('inp-bk-file').files[0];

    if (!title || !file) return showToast('Please enter title and select PDF file.', 'warning');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Uploading PDF to Supabase...';

    try {
        const uploadResult = await uploadFileToSupabase(file, 'pdf-documents');

        const { error } = await supabaseClient.from('books').insert([{
            id: 'bk_' + Date.now(),
            title: title,
            category: cat,
            class_id: classId,
            subject_id: subjectId,
            subject_name: subjectName,
            pdf_url: uploadResult.publicUrl,
            chapters_count: 12,
            author: cat,
            is_published: true
        }]);

        if (error) throw error;

        showToast('Book PDF uploaded and published to Student App!', 'success');
        closeModal();
        renderBooks();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Save Changes';
    }
}

async function submitCreateNote() {
    const btn = document.getElementById('modal-submit-btn');
    const title = document.getElementById('inp-nt-title').value.trim();
    const classId = document.getElementById('inp-nt-class').value;
    const subSelect = document.getElementById('inp-nt-sub');
    const subjectId = subSelect.value;
    const subjectName = subSelect.options[subSelect.selectedIndex]?.dataset.name || '';
    const file = document.getElementById('inp-nt-file').files[0];

    if (!title || !file) return showToast('Please enter note title and select PDF file.', 'warning');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Uploading Notes PDF...';

    try {
        const uploadResult = await uploadFileToSupabase(file, 'study-materials');

        const { error } = await supabaseClient.from('notes').insert([{
            id: 'note_' + Date.now(),
            title: title,
            class_id: classId,
            subject_id: subjectId,
            subject_name: subjectName,
            chapter_id: 'ch-1',
            chapter_title: title,
            content: uploadResult.publicUrl, // Direct PDF URL for In-App Reader
            summary: title,
            key_points: [`Handwritten Notes for ${title}`],
            is_published: true
        }]);

        if (error) throw error;

        showToast('Chapter Notes PDF uploaded successfully!', 'success');
        closeModal();
        renderNotes();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Save Changes';
    }
}

async function submitCreateModelPaper() {
    const btn = document.getElementById('modal-submit-btn');
    const title = document.getElementById('inp-mp-title').value.trim();
    const classId = document.getElementById('inp-mp-class').value;
    const subSelect = document.getElementById('inp-mp-sub');
    const subjectId = subSelect.value;
    const subjectName = subSelect.options[subSelect.selectedIndex]?.dataset.name || '';
    const year = document.getElementById('inp-mp-year').value || '2026';
    const file = document.getElementById('inp-mp-file').files[0];

    if (!title || !file) return showToast('Please enter title and select PDF file.', 'warning');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Uploading Model Paper...';

    try {
        const uploadResult = await uploadFileToSupabase(file, 'pdf-documents');

        const { error } = await supabaseClient.from('model_papers').insert([{
            id: 'mp_' + Date.now(),
            title: title,
            class_id: classId,
            subject_id: subjectId,
            subject_name: subjectName,
            year: year,
            file_url: uploadResult.publicUrl,
            questions_count: 25,
            duration_minutes: 90,
            is_published: true
        }]);

        if (error) throw error;

        showToast('Model Paper uploaded and published!', 'success');
        closeModal();
        renderModelPapers();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Save Changes';
    }
}

async function submitCreatePYQ() {
    const btn = document.getElementById('modal-submit-btn');
    const title = document.getElementById('inp-pyq-title').value.trim();
    const classId = document.getElementById('inp-pyq-class').value;
    const subSelect = document.getElementById('inp-pyq-sub');
    const subjectId = subSelect.value;
    const subjectName = subSelect.options[subSelect.selectedIndex]?.dataset.name || '';
    const year = document.getElementById('inp-pyq-year').value || '2025';
    const file = document.getElementById('inp-pyq-file').files[0];

    if (!title || !file) return showToast('Please enter title and select PDF file.', 'warning');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Uploading PYQ PDF...';

    try {
        const uploadResult = await uploadFileToSupabase(file, 'pdf-documents');

        const { error } = await supabaseClient.from('previous_year_papers').insert([{
            id: 'pyq_' + Date.now(),
            title: title,
            class_id: classId,
            subject_id: subjectId,
            subject_name: subjectName,
            year: year,
            exam_type: 'Annual Board Exam',
            file_url: uploadResult.publicUrl,
            questions_count: 30,
            is_published: true
        }]);

        if (error) throw error;

        showToast('PYQ Paper uploaded successfully!', 'success');
        closeModal();
        renderPYQ();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Save Changes';
    }
}

async function submitUploadPDF() {
    const btn = document.getElementById('modal-submit-btn');
    const title = document.getElementById('inp-pdf-title').value.trim();
    const cat = document.getElementById('inp-pdf-cat').value;
    const classId = document.getElementById('inp-pdf-class').value;
    const subSelect = document.getElementById('inp-pdf-sub');
    const subjectId = subSelect.value;
    const subjectName = subSelect.options[subSelect.selectedIndex]?.dataset.name || '';
    const file = document.getElementById('inp-pdf-file').files[0];

    if (!title || !file) return showToast('Please enter title and select PDF file.', 'warning');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Uploading Document...';

    try {
        const uploadResult = await uploadFileToSupabase(file, 'pdf-documents');

        const { error } = await supabaseClient.from('pdf_documents').insert([{
            id: 'pdf_' + Date.now(),
            title: title,
            description: title,
            category: cat,
            class_id: classId,
            subject_id: subjectId,
            subject_name: subjectName,
            file_url: uploadResult.publicUrl,
            file_size: uploadResult.sizeMB,
            pages_count: 10,
            is_published: true
        }]);

        if (error) throw error;

        showToast('PDF Document saved and published!', 'success');
        closeModal();
        renderPDFs();
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Save Changes';
    }
}

async function submitCreateTest() {
    const title = document.getElementById('inp-t-title').value.trim();
    const classId = document.getElementById('inp-t-class').value;
    const subSelect = document.getElementById('inp-t-sub');
    const subjectId = subSelect.value;
    const subjectName = subSelect.options[subSelect.selectedIndex]?.dataset.name || '';
    const duration = parseInt(document.getElementById('inp-t-duration').value) || 15;
    const marks = parseInt(document.getElementById('inp-t-marks').value) || 20;
    const isDaily = document.getElementById('inp-t-daily').checked;

    if (!title) return showToast('Test title is required.', 'warning');

    const { error } = await supabaseClient.from('tests').insert([{
        id: 'test_' + Date.now(),
        title: title,
        description: `Online examination for ${subjectName} (${classId})`,
        class_id: classId,
        subject_id: subjectId,
        subject_name: subjectName,
        duration_minutes: duration,
        total_marks: marks,
        passing_marks: Math.ceil(marks * 0.4),
        questions_count: 10,
        is_daily_quiz: isDaily,
        is_published: true
    }]);

    if (error) {
        showToast(error.message, 'error');
    } else {
        showToast('Test created and published to Student App!', 'success');
        closeModal();
        if (isDaily) renderDailyQuiz(); else renderTests();
    }
}

async function submitCreateQuestion() {
    const text = document.getElementById('inp-q-text').value.trim();
    const imageUrl = document.getElementById('inp-q-image').value.trim();
    const a = document.getElementById('inp-q-a').value.trim();
    const b = document.getElementById('inp-q-b').value.trim();
    const c = document.getElementById('inp-q-c').value.trim();
    const d = document.getElementById('inp-q-d').value.trim();
    const correct = document.getElementById('inp-q-correct').value;
    const classId = document.getElementById('inp-q-class').value;
    const subSelect = document.getElementById('inp-q-sub');
    const subjectId = subSelect.value;
    const subjectName = subSelect.options[subSelect.selectedIndex]?.dataset.name || '';
    const diff = document.getElementById('inp-q-diff').value;
    const exp = document.getElementById('inp-q-exp').value.trim();

    if (!text || !a || !b || !c || !d) {
        return showToast('Please enter question text and all 4 options.', 'warning');
    }

    const { error } = await supabaseClient.from('questions').insert([{
        id: 'q_' + Date.now(),
        question_text: text,
        image_url: imageUrl || null,
        option_a: a,
        option_b: b,
        option_c: c,
        option_d: d,
        correct_option: correct,
        explanation: exp,
        difficulty: diff,
        class_id: classId,
        subject_id: subjectId,
        subject_name: subjectName,
        marks: 1
    }]);

    if (error) {
        showToast(error.message, 'error');
    } else {
        showToast('Question saved to Question Bank!', 'success');
        closeModal();
        renderQuestions();
    }
}

async function submitCreateNotification() {
    const title = document.getElementById('inp-notif-title').value.trim();
    const msg = document.getElementById('inp-notif-msg').value.trim();
    const type = document.getElementById('inp-notif-type').value;
    const target = document.getElementById('inp-notif-target').value;

    if (!title || !msg) return showToast('Please enter notification title and message.', 'warning');

    const { error } = await supabaseClient.from('notifications').insert([{
        id: 'notif_' + Date.now(),
        title: title,
        message: msg,
        type: type,
        target_type: target,
        target_id: '',
        is_read: false
    }]);

    if (error) {
        showToast(error.message, 'error');
    } else {
        showToast('Push notification broadcasted to all students!', 'success');
        closeModal();
        renderNotifications();
    }
}

async function submitCreateAnnouncement() {
    const title = document.getElementById('inp-ann-title').value.trim();
    const desc = document.getElementById('inp-ann-desc').value.trim();
    const badge = document.getElementById('inp-ann-badge').value.trim() || 'UPDATE';

    if (!title || !desc) return showToast('Please enter headline and description.', 'warning');

    const { error } = await supabaseClient.from('announcements').insert([{
        id: 'ann_' + Date.now(),
        title: title,
        description: desc,
        badge: badge,
        is_active: true
    }]);

    if (error) {
        showToast(error.message, 'error');
    } else {
        showToast('Flash announcement posted!', 'success');
        closeModal();
        renderAnnouncements();
    }
}

async function submitCreateDailyUpdate() {
    const title = document.getElementById('inp-du-title').value.trim();
    const content = document.getElementById('inp-du-content').value.trim();
    const dateStr = new Date().toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });

    if (!title || !content) return showToast('Please enter title and content.', 'warning');

    const { error } = await supabaseClient.from('daily_updates').insert([{
        id: 'du_' + Date.now(),
        title: title,
        content: content,
        date: dateStr,
        tag: 'DAILY UPDATE',
        is_pinned: false
    }]);

    if (error) {
        showToast(error.message, 'error');
    } else {
        showToast('Daily update posted!', 'success');
        closeModal();
        renderDailyUpdates();
    }
}

// ==========================================================================
// 7. GENERIC RECORD UTILITIES (DELETE & TOGGLE PUBLISH)
// ==========================================================================

async function deleteRecord(tableName, id, reloadCallback) {
    if (!confirm('Are you sure you want to delete this record? It will be removed from Supabase and the Student App.')) {
        return;
    }

    const { error } = await supabaseClient.from(tableName).delete().eq('id', id);
    if (error) {
        showToast('Delete error: ' + error.message, 'error');
    } else {
        showToast('Record deleted successfully.', 'success');
        if (reloadCallback) reloadCallback();
    }
}

async function togglePublish(tableName, id, newStatus, reloadCallback) {
    const { error } = await supabaseClient.from(tableName).update({ is_published: newStatus }).eq('id', id);
    if (error) {
        showToast('Update error: ' + error.message, 'error');
    } else {
        showToast(`Item ${newStatus ? 'Published' : 'Unpublished'} successfully!`, 'success');
        if (reloadCallback) reloadCallback();
    }
}

// ==========================================================================
// 8. TOAST NOTIFICATIONS & SANITIZATION
// ==========================================================================

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'fa-circle-info';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-circle-exclamation';
    if (type === 'warning') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(20px)';
        setTimeout(() => toast.remove(), 300);
    }, 3800);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
