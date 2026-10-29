import Link from "next/link";
import { ArrowDownIcon, ArrowUpIcon, ChevronsUpDownIcon } from "lucide-react";
import { PriorityLabel, SeverityBadge, StatusBadge } from "@/components/incident-badges";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import type { Catalog } from "@/lib/catalog";
import { formatDateTime } from "@/lib/format";
import { incidentsHref, type IncidentQuery, type SortKey } from "@/lib/incident-query";
import type { Incident } from "@/lib/incidents";

function SortHeader({ query, column, children }: { query: IncidentQuery; column: SortKey; children: string }) {
  const active = query.sort === column;
  // Clicking the active column flips the order; a new column starts newest/A-first.
  const order = active ? (query.order === "asc" ? "desc" : "asc") : column === "createdAt" ? "desc" : "asc";
  const Icon = !active ? ChevronsUpDownIcon : query.order === "asc" ? ArrowUpIcon : ArrowDownIcon;
  return (
    <TableHead aria-sort={active ? (query.order === "asc" ? "ascending" : "descending") : undefined}>
      <Link href={incidentsHref({ ...query, sort: column, order, page: 1 })} className="inline-flex items-center gap-1 hover:underline">
        {children}
        <Icon className={active ? "size-3.5" : "size-3.5 text-muted-foreground/60"} />
      </Link>
    </TableHead>
  );
}

type Props = { incidents: Incident[]; catalog: Catalog; query: IncidentQuery; timeZone?: string };

export function IncidentTable({ incidents, catalog, query, timeZone }: Props) {
  return (
    <div className="rounded-lg border bg-background">
      <Table>
        <TableHeader>
          <TableRow>
            <SortHeader query={query} column="title">Title</SortHeader>
            <TableHead>Reporter</TableHead>
            <TableHead>Category</TableHead>
            <TableHead>Site</TableHead>
            <TableHead>Severity</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Priority</TableHead>
            <SortHeader query={query} column="assignee">Assignee</SortHeader>
            <SortHeader query={query} column="createdAt">Created</SortHeader>
          </TableRow>
        </TableHeader>
        <TableBody>
          {incidents.length === 0 && (
            <TableRow>
              <TableCell colSpan={9} className="py-10 text-center text-muted-foreground">
                No incidents match these filters.
              </TableCell>
            </TableRow>
          )}
          {incidents.map((incident) => (
            <TableRow key={incident.id}>
              <TableCell className="max-w-72 truncate font-medium">
                <Link href={`/incidents/${incident.id}`} className="hover:underline">
                  {incident.title}
                </Link>
              </TableCell>
              <TableCell>{catalog.userName(incident.reporterId)}</TableCell>
              <TableCell>{catalog.categoryName(incident.categoryId)}</TableCell>
              <TableCell>{catalog.siteName(incident.siteId)}</TableCell>
              <TableCell>
                <SeverityBadge severity={catalog.severity(incident.severityId)} />
              </TableCell>
              <TableCell>
                <StatusBadge status={incident.status} />
              </TableCell>
              <TableCell>
                <PriorityLabel priority={incident.priority} />
              </TableCell>
              <TableCell>{incident.assignee ?? <span className="text-muted-foreground">—</span>}</TableCell>
              <TableCell className="tabular-nums">{formatDateTime(incident.createdAt, timeZone)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
