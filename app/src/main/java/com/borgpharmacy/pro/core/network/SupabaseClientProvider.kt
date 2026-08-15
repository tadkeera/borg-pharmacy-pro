package com.borgpharmacy.pro.core.network
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import com.borgpharmacy.pro.BuildConfig
object SupabaseClientProvider { val client by lazy { createSupabaseClient(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_ANON_KEY){ install(Postgrest); install(Realtime); install(Storage) } } }
