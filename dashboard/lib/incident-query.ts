import { startOfLocalDay } from "./format";
import { isPriority, isStatus, type Priority, type Status } from "./incidents";

/** Columns the server can sort meaningfully (its sort compares the stored values). */
export const SORT_KEYS = ["title", "assignee", "createdAt"] as const;
export type SortKey = (typeof SORT_KEYS)[number];
export type SortOrder = "asc" | "desc";

export const PAGE_SIZES = [5, 10, 25, 50] as const;
const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_SORT: SortKey = "createdAt";
const DEFAULT_ORDER: SortOrder = "desc";

/** The /incidents table's filter, sort and page state, all kept in the URL. */
export type IncidentQuery = {
  status?: Status;
  severity?: string;
  priority?: Priority;
  category?: string;
  site?: string;
  assignee?: string;
  from?: string; // YYYY-MM-DD, created on or after (local day)
  to?: string; // YYYY-MM-DD, created on or before (local day)
  q?: string;
  sort: SortKey;
  order: SortOrder;
  page: number;
  limit: number;
};

export type SearchParams = Record<string, string | string[] | undefined>;

const DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Reads the query from URL search params, ignoring anything malformed. */
export function parseIncidentQuery(sp: SearchParams): IncidentQuery {
  const one = (key: string) => {
    const v = sp[key];
    const s = (Array.isArray(v) ? v[0] : v)?.trim();
    return s ? s : undefined;
  };
  const int = (key: string) => Number.parseInt(one(key) ?? "", 10);
  const sort = one("sort");
  const limit = int("limit");
  return {
    status: isStatus(one("status")) ? (one("status") as Status) : undefined,
    severity: one("severity"),
    priority: isPriority(one("priority")) ? (one("priority") as Priority) : undefined,
    category: one("category"),
    site: one("site"),
    assignee: one("assignee"),
    from: DATE.test(one("from") ?? "") ? one("from") : undefined,
    to: DATE.test(one("to") ?? "") ? one("to") : undefined,
    q: one("q"),
    sort: SORT_KEYS.includes(sort as SortKey) ? (sort as SortKey) : DEFAULT_SORT,
    order: one("order") === "asc" || one("order") === "desc" ? (one("order") as SortOrder) : DEFAULT_ORDER,
    page: int("page") >= 1 ? int("page") : 1,
    limit: (PAGE_SIZES as readonly number[]).includes(limit) ? limit : DEFAULT_PAGE_SIZE,
  };
}

/** The dashboard URL for `query`, leaving out defaults so links stay short. */
export function incidentsHref(query: IncidentQuery): string {
  const p = new URLSearchParams();
  const filters = ["status", "severity", "priority", "category", "site", "assignee", "from", "to", "q"] as const;
  for (const key of filters) if (query[key]) p.set(key, query[key]);
  if (query.sort !== DEFAULT_SORT || query.order !== DEFAULT_ORDER) {
    p.set("sort", query.sort);
    p.set("order", query.order);
  }
  if (query.limit !== DEFAULT_PAGE_SIZE) p.set("limit", String(query.limit));
  if (query.page > 1) p.set("page", String(query.page));
  const s = p.toString();
  return s ? `/incidents?${s}` : "/incidents";
}

const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

/** json-server 0.17 parameters for GET /incidents. */
export function toServerParams(query: IncidentQuery, timeZone?: string): URLSearchParams {
  const p = new URLSearchParams();
  if (query.status) p.set("status", query.status);
  if (query.severity) p.set("severityId", query.severity);
  if (query.priority) p.set("priority", query.priority);
  if (query.category) p.set("categoryId", query.category);
  if (query.site) p.set("siteId", query.site);
  // Case-insensitive "contains" match, so "plumbing" finds "Plumbing crew".
  if (query.assignee) p.set("assignee_like", escapeRegExp(query.assignee));
  if (query.q) p.set("q", query.q);
  const from = query.from && startOfLocalDay(query.from, timeZone);
  const toExclusive = query.to && startOfLocalDay(query.to, timeZone, 1);
  if (from) p.set("createdAt_gte", String(from));
  if (toExclusive) p.set("createdAt_lte", String(toExclusive - 1));
  p.set("_sort", query.sort);
  p.set("_order", query.order);
  p.set("_page", String(query.page));
  p.set("_limit", String(query.limit));
  return p;
}
