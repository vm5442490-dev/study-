// SUPER STUDY ADMIN CONFIG
// Connects to the same Supabase project as the Android Student App

window.SUPABASE_CONFIG = {
    url: "https://wbieadcwhteohabvrphh.supabase.co",
    anonKey: "sb_publishable_PoFac7Jv1vw4E4epfaxzsQ_rs4JLizz",
    storageBuckets: {
        pdfDocuments: "pdf-documents",
        studyMaterials: "study-materials"
    }
};

// Initialize Supabase Client
window.supabaseClient = window.supabase.createClient(
    window.SUPABASE_CONFIG.url,
    window.SUPABASE_CONFIG.anonKey
);
