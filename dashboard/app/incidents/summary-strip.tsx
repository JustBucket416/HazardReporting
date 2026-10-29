import Link from "next/link";
import { apiFetch } from "@/lib/api";
import { NON_FINAL_STATUSES, type Severity } from "@/lib/incidents";
import { cn } from "@/lib/utils";

/** Number of incidents matching `params`, read from X-Total-Count with a one-row page. */
async function countIncidents(params: [string, string][]): Promise<number> {
  const p = new URLSearchParams([...params, ["_limit", "1"]]);
  const res = await apiFetch(`/incidents?${p}`);
  return Number(res.headers.get("X-Total-Count") ?? 0);
}

/** Counts of Open, In Progress and Critical-severity non-final incidents. */
export async function SummaryStrip({ severities }: { severities: Severity[] }) {
  const critical = severities.at(-1); // highest rank
  const [open, inProgress, criticalActive] = await Promise.all([
    countIncidents([["status", "open"]]),
    countIncidents([["status", "in_progress"]]),
    critical
      ? countIncidents([["severityId", critical.id], ...NON_FINAL_STATUSES.map((s): [string, string] => ["status", s])])
      : 0,
  ]);

  const items = [
    { label: "Open", count: open, href: "/incidents?status=open" },
    { label: "In Progress", count: inProgress, href: "/incidents?status=in_progress" },
    { label: `${critical?.name ?? "Critical"} severity, not final`, count: criticalActive, alert: criticalActive > 0 },
  ];

  return (
    <section aria-label="Summary" className="grid grid-cols-1 gap-3 sm:grid-cols-3">
      {items.map((item) => {
        const body = (
          <>
            <p className="text-sm text-muted-foreground">{item.label}</p>
            <p className={cn("text-2xl font-semibold tabular-nums", item.alert && "text-red-700")}>{item.count}</p>
          </>
        );
        const box = "rounded-lg border bg-background px-4 py-3";
        return item.href ? (
          <Link key={item.label} href={item.href} className={cn(box, "hover:bg-muted/50")}>
            {body}
          </Link>
        ) : (
          <div key={item.label} className={box}>
            {body}
          </div>
        );
      })}
    </section>
  );
}
