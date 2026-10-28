import { Suspense } from "react";
import { apiFetch } from "@/lib/api";

async function IncidentCount() {
  const res = await apiFetch("/incidents?_limit=1");
  return <p className="text-sm text-muted-foreground">{res.headers.get("X-Total-Count") ?? 0} incidents</p>;
}

export default function IncidentsPage() {
  return (
    <div className="grid gap-4">
      <h1 className="text-xl font-semibold">Incidents</h1>
      <Suspense fallback={<p className="text-sm text-muted-foreground">Loading…</p>}>
        <IncidentCount />
      </Suspense>
    </div>
  );
}
