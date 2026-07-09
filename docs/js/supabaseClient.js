// Ligação à SUPABASE

const SUPERBASE_URL = "https://aacjetgkrxbprluscivm.supabase.co";
const SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFhY2pldGdrcnhicHJsdXNjaXZtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI4MDY3MTUsImV4cCI6MjA5ODM4MjcxNX0.Pb07wajgIDfzRfEkjaTWyaM2_e1IcYhR4FC0oXG0cBA";


//const SUPERBASE_URL = "https://blmldtfejhrjyllcqtwv.supabase.co";
//const SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJsbWxkdGZlamhyanlsbGNxdHd2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODMwNzM5NTQsImV4cCI6MjA5ODY0OTk1NH0.vvnSEwgmsKctZeUZTmDUKeWigscCrHEU14HJJGMw7eU";


const supabaseClient = window.supabase.createClient(SUPERBASE_URL, SUPABASE_ANON_KEY, {
    auth: {
        persistSession: false,
        autoRefreshToken: true,
        detectSessionInUrl: false,
    },
});