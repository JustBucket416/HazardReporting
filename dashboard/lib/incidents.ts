// Vocabulary follows GLOSSARY.md; shapes follow spec § 1 "Collections".

export const STATUSES = ["open", "acknowledged", "in_progress", "resolved", "rejected", "withdrawn"] as const;
export type Status = (typeof STATUSES)[number];

export const PRIORITIES = ["low", "medium", "high", "urgent"] as const;
export type Priority = (typeof PRIORITIES)[number];

export const STATUS_LABELS: Record<Status, string> = {
  open: "Open",
  acknowledged: "Acknowledged",
  in_progress: "In Progress",
  resolved: "Resolved",
  rejected: "Rejected",
  withdrawn: "Withdrawn",
};

export const PRIORITY_LABELS: Record<Priority, string> = {
  low: "Low",
  medium: "Medium",
  high: "High",
  urgent: "Urgent",
};

/** Statuses an operator may move an incident to (spec § 1 "Operator routes"). */
export const TRANSITIONS: Partial<Record<Status, readonly Status[]>> = {
  open: ["acknowledged", "rejected"],
  acknowledged: ["in_progress", "rejected"],
  in_progress: ["resolved", "rejected"],
};

/** Statuses in which an incident is read-only. */
export const FINAL_STATUSES: readonly Status[] = ["resolved", "rejected", "withdrawn"];
export const NON_FINAL_STATUSES = STATUSES.filter((s) => !FINAL_STATUSES.includes(s));

export const isStatus = (v: unknown): v is Status => STATUSES.includes(v as Status);
export const isPriority = (v: unknown): v is Priority => PRIORITIES.includes(v as Priority);
export const isFinal = (status: Status) => FINAL_STATUSES.includes(status);

export type Note = { at: number; byUserId: string; text: string };

export type Incident = {
  id: string;
  reporterId: string;
  title: string;
  description: string;
  categoryId: string;
  severityId: string;
  siteId: string;
  locationDescription: string;
  position: { lat: number; lng: number; accuracyM: number } | null;
  photoUrls: string[];
  status: Status;
  priority: Priority | null;
  assignee: string | null;
  notes: Note[];
  createdAt: number;
  receivedAt: number;
  updatedAt: number;
};

export type Category = { id: string; name: string };
export type Severity = { id: string; name: string; rank: number };
export type Site = { id: string; name: string; lat: number; lng: number };
export type User = { id: string; email: string; name: string; role: string };
