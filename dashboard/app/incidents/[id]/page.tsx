import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense, type ReactNode } from "react";
import { ArrowLeftIcon, LockIcon } from "lucide-react";
import { PriorityLabel, SeverityBadge, StatusBadge } from "@/components/incident-badges";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ApiError, apiGet, serverUrl } from "@/lib/api";
import { getCatalog } from "@/lib/catalog";
import { formatDateTime } from "@/lib/format";
import { isFinal, STATUS_LABELS, TRANSITIONS, type Incident } from "@/lib/incidents";
import { getTimeZone } from "@/lib/session";
import { MapLoader } from "./map-loader";
import { NoteForm } from "./note-form";
import { TriageForm } from "./triage-form";

async function getIncident(id: string): Promise<Incident> {
  try {
    return await apiGet<Incident>(`/incidents/${encodeURIComponent(id)}`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }
}

function Field({ label, children, wide }: { label: string; children: ReactNode; wide?: boolean }) {
  return (
    <div className={wide ? "grid gap-0.5 sm:col-span-2" : "grid gap-0.5"}>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-sm">{children}</dd>
    </div>
  );
}

const none = <span className="text-muted-foreground">—</span>;

async function IncidentDetail({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [incident, catalog, timeZone] = await Promise.all([getIncident(id), getCatalog(), getTimeZone()]);
  const reporter = catalog.users.find((u) => u.id === incident.reporterId);
  const final = isFinal(incident.status);
  const date = (ms: number) => formatDateTime(ms, timeZone);

  return (
    <div className="grid grid-cols-1 gap-6">
      <div className="grid gap-2">
        <h1 className="text-xl font-semibold">{incident.title}</h1>
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={incident.status} />
          <SeverityBadge severity={catalog.severity(incident.severityId)} />
          <span className="text-sm text-muted-foreground">Reported {date(incident.createdAt)}</span>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="grid min-w-0 content-start gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Photos</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              {incident.photoUrls.map((url, i) => (
                // Photos are served by the backend; next/image's optimizer refuses local hosts.
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  key={url}
                  src={serverUrl(url)}
                  alt={`Photo ${i + 1} of ${incident.title}`}
                  className="aspect-[4/3] w-full rounded-lg border bg-muted object-cover"
                />
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4">
              <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field label="Description" wide>
                  <p className="whitespace-pre-wrap">{incident.description || none}</p>
                </Field>
                <Field label="Reporter">
                  {reporter ? `${reporter.name} (${reporter.email})` : catalog.userName(incident.reporterId)}
                </Field>
                <Field label="Category">{catalog.categoryName(incident.categoryId)}</Field>
                <Field label="Severity">{catalog.severity(incident.severityId)?.name ?? none}</Field>
                <Field label="Site">{catalog.siteName(incident.siteId)}</Field>
                <Field label="Location description">{incident.locationDescription || none}</Field>
                <Field label="Position">
                  {incident.position
                    ? `${incident.position.lat.toFixed(5)}, ${incident.position.lng.toFixed(5)} (±${Math.round(incident.position.accuracyM)} m)`
                    : "Not captured"}
                </Field>
                <Field label="Reported">{date(incident.createdAt)}</Field>
                <Field label="Received">{date(incident.receivedAt)}</Field>
                <Field label="Last updated">{date(incident.updatedAt)}</Field>
              </dl>
              {incident.position && <MapLoader {...incident.position} />}
            </CardContent>
          </Card>
        </div>

        <div className="grid min-w-0 content-start gap-6">
          <Card>
            <CardHeader>
              <CardTitle>Triage</CardTitle>
            </CardHeader>
            <CardContent>
              {final ? (
                <div className="grid gap-4">
                  <p className="flex items-center gap-2 rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
                    <LockIcon className="size-4 shrink-0" />
                    {STATUS_LABELS[incident.status]} is final; this incident can no longer be changed.
                  </p>
                  <dl className="grid gap-3">
                    <Field label="Status">{STATUS_LABELS[incident.status]}</Field>
                    <Field label="Priority">
                      <PriorityLabel priority={incident.priority} />
                    </Field>
                    <Field label="Assignee">{incident.assignee ?? none}</Field>
                  </dl>
                </div>
              ) : (
                <TriageForm
                  incidentId={incident.id}
                  status={incident.status}
                  transitions={TRANSITIONS[incident.status] ?? []}
                  priority={incident.priority}
                  assignee={incident.assignee}
                  version={incident.updatedAt}
                />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Notes</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4">
              {incident.notes.length === 0 ? (
                <p className="text-sm text-muted-foreground">No notes yet.</p>
              ) : (
                <ol className="grid gap-3">
                  {incident.notes.map((note, i) => (
                    <li key={`${note.at}-${i}`} className="rounded-lg border bg-muted/40 px-3 py-2">
                      <p className="text-xs text-muted-foreground">
                        <span className="font-medium text-foreground">{catalog.userName(note.byUserId)}</span> ·{" "}
                        <time dateTime={new Date(note.at).toISOString()}>{date(note.at)}</time>
                      </p>
                      <p className="mt-1 text-sm whitespace-pre-wrap">{note.text}</p>
                    </li>
                  ))}
                </ol>
              )}
              {!final && <NoteForm incidentId={incident.id} />}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

export default function IncidentPage({ params }: PageProps<"/incidents/[id]">) {
  return (
    <div className="grid gap-4">
      <Link href="/incidents" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:underline">
        <ArrowLeftIcon className="size-4" />
        All incidents
      </Link>
      <Suspense fallback={<p className="text-sm text-muted-foreground">Loading incident…</p>}>
        <IncidentDetail params={params} />
      </Suspense>
    </div>
  );
}
