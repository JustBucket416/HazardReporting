import { Suspense } from "react";
import { apiFetch } from "@/lib/api";
import { getCatalog } from "@/lib/catalog";
import { parseIncidentQuery, toServerParams, type SearchParams } from "@/lib/incident-query";
import type { Incident } from "@/lib/incidents";
import { getTimeZone } from "@/lib/session";
import { FilterBar } from "./filter-bar";
import { IncidentTable } from "./incident-table";
import { Pagination } from "./pagination";
import { SummaryStrip } from "./summary-strip";

async function IncidentsView({ searchParams }: { searchParams: Promise<SearchParams> }) {
  const query = parseIncidentQuery(await searchParams);
  const timeZone = await getTimeZone();
  const [catalog, res] = await Promise.all([
    getCatalog(),
    apiFetch(`/incidents?${toServerParams(query, timeZone)}`),
  ]);
  const incidents = (await res.json()) as Incident[];
  const total = Number(res.headers.get("X-Total-Count") ?? incidents.length);

  return (
    <>
      <SummaryStrip severities={catalog.severities} />
      <FilterBar query={query} categories={catalog.categories} severities={catalog.severities} sites={catalog.sites} />
      <IncidentTable incidents={incidents} catalog={catalog} query={query} timeZone={timeZone} />
      <Pagination query={query} total={total} />
    </>
  );
}

export default function IncidentsPage({ searchParams }: PageProps<"/incidents">) {
  return (
    <div className="grid grid-cols-1 gap-4">
      <h1 className="text-xl font-semibold">Incidents</h1>
      <Suspense fallback={<p className="text-sm text-muted-foreground">Loading incidents…</p>}>
        <IncidentsView searchParams={searchParams} />
      </Suspense>
    </div>
  );
}
