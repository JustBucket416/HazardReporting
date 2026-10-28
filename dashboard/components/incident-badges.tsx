import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { PRIORITY_LABELS, STATUS_LABELS, type Priority, type Severity, type Status } from "@/lib/incidents";

const STATUS_STYLES: Record<Status, string> = {
  open: "bg-blue-100 text-blue-800",
  acknowledged: "bg-violet-100 text-violet-800",
  in_progress: "bg-amber-100 text-amber-800",
  resolved: "bg-green-100 text-green-800",
  rejected: "bg-zinc-100 text-zinc-600 border-zinc-300",
  withdrawn: "bg-zinc-100 text-zinc-500 border-dashed border-zinc-300",
};

export function StatusBadge({ status }: { status: Status }) {
  return <Badge className={cn("font-medium", STATUS_STYLES[status])}>{STATUS_LABELS[status]}</Badge>;
}

/** Severity by rank: Low neutral, Medium amber, High orange, Critical red. */
const SEVERITY_STYLES: Record<number, string> = {
  1: "bg-zinc-100 text-zinc-700",
  2: "bg-amber-100 text-amber-800",
  3: "bg-orange-100 text-orange-800",
  4: "bg-red-600 text-white",
};

export function SeverityBadge({ severity }: { severity?: Severity }) {
  if (!severity) return <span className="text-muted-foreground">—</span>;
  return <Badge className={SEVERITY_STYLES[severity.rank] ?? SEVERITY_STYLES[1]}>{severity.name}</Badge>;
}

export function PriorityLabel({ priority }: { priority: Priority | null }) {
  if (!priority) return <span className="text-muted-foreground">—</span>;
  return <span className={cn(priority === "urgent" && "font-semibold text-red-700")}>{PRIORITY_LABELS[priority]}</span>;
}
