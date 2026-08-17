// Authenticated, tenant-scoped synchronization endpoint.
// Deploy with: supabase functions deploy secure-sync
// Required secrets are managed by Supabase: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const allowedRoles = new Set(["OWNER", "ADMIN", "PHARMACIST", "EMPLOYEE", "REPRESENTATIVE", "VIEWER"]);
const writableRoles = new Set(["OWNER", "ADMIN", "PHARMACIST"]);
const allowedEntities = new Set(["COMPANY", "REPRESENTATIVE", "VISIT", "USER"]);
const allowedOperations = new Set(["UPSERT", "SOFT_DELETE"]);

type SyncOperation = {
  idempotencyKey: string;
  operation: "UPSERT" | "SOFT_DELETE";
  entityType: "COMPANY" | "REPRESENTATIVE" | "VISIT" | "USER";
  entityId: string;
  payload: Record<string, unknown>;
  version: number;
};

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "POST required" }, 405);

  try {
    const supabaseUrl = requiredEnv("SUPABASE_URL");
    const serviceRole = requiredEnv("SUPABASE_SERVICE_ROLE_KEY");
    const token = bearer(req.headers.get("Authorization"));
    if (!token) return json({ error: "Missing bearer token" }, 401);

    const admin = createClient(supabaseUrl, serviceRole, {
      auth: { autoRefreshToken: false, persistSession: false },
    });
    const { data: authData, error: authError } = await admin.auth.getUser(token);
    if (authError || !authData.user) return json({ error: "Invalid caller token" }, 401);

    const { data: profile, error: profileError } = await admin
      .from("user_profiles")
      .select("tenant_id, role_v2, role, active")
      .eq("user_id", authData.user.id)
      .single();
    if (profileError || !profile?.active) return json({ error: "Inactive or missing profile" }, 403);

    const role = String(profile.role_v2 ?? profile.role ?? "VIEWER").toUpperCase();
    if (!allowedRoles.has(role)) return json({ error: "Unknown role" }, 403);
    if (!writableRoles.has(role)) return json({ error: "Role cannot synchronize writes" }, 403);

    const body = await req.json() as { operations?: SyncOperation[] };
    const operations = Array.isArray(body.operations) ? body.operations : [];
    if (operations.length === 0 || operations.length > 100) {
      return json({ error: "Provide between 1 and 100 operations" }, 400);
    }

    const rows = operations.map((operation) => {
      validateOperation(operation);
      const payload = { ...operation.payload };
      // The server is authoritative for tenant and actor identity.
      delete payload.tenant_id;
      delete payload.tenantId;
      delete payload.actor_user_id;
      return {
        tenant_id: profile.tenant_id,
        idempotency_key: operation.idempotencyKey,
        actor_user_id: authData.user.id,
        operation: operation.operation,
        entity_type: operation.entityType,
        entity_id: operation.entityId,
        payload: { ...payload, tenant_id: profile.tenant_id },
        version: operation.version,
      };
    });

    const { data: accepted, error: insertError } = await admin
      .from("sync_operations")
      .upsert(rows, { onConflict: "tenant_id,idempotency_key", ignoreDuplicates: true })
      .select("idempotency_key, entity_type, entity_id, version");
    if (insertError) return json({ error: insertError.message }, 500);

    return json({ tenantId: profile.tenant_id, accepted: accepted ?? [] });
  } catch (error) {
    return json({ error: error instanceof Error ? error.message : String(error) }, 400);
  }
});

function validateOperation(operation: SyncOperation) {
  if (!operation || typeof operation.idempotencyKey !== "string" || operation.idempotencyKey.length < 16 || operation.idempotencyKey.length > 128) {
    throw new Error("Invalid idempotency key");
  }
  if (!allowedOperations.has(operation.operation)) throw new Error("Invalid operation");
  if (!allowedEntities.has(operation.entityType)) throw new Error("Invalid entity type");
  if (!operation.entityId || operation.entityId.length > 128) throw new Error("Invalid entity id");
  if (!operation.payload || typeof operation.payload !== "object") throw new Error("Invalid payload");
  if (!Number.isSafeInteger(operation.version) || operation.version < 0) throw new Error("Invalid version");
}

function bearer(value: string | null): string | null {
  if (!value?.startsWith("Bearer ")) return null;
  const token = value.slice("Bearer ".length).trim();
  return token.length > 0 ? token : null;
}

function requiredEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing environment variable: ${name}`);
  return value;
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
