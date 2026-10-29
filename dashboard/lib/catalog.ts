import { apiGet } from "./api";
import type { Category, Severity, Site, User } from "./incidents";

export type Catalog = {
  categories: Category[];
  severities: Severity[];
  sites: Site[];
  users: User[];
  categoryName: (id: string) => string;
  severity: (id: string) => Severity | undefined;
  siteName: (id: string) => string;
  userName: (id: string) => string;
};

/** Categories, severities (by rank), sites and users, with id → name lookups. */
export async function getCatalog(): Promise<Catalog> {
  const [categories, severities, sites, users] = await Promise.all([
    apiGet<Category[]>("/categories"),
    apiGet<Severity[]>("/severities"),
    apiGet<Site[]>("/sites"),
    apiGet<User[]>("/users"),
  ]);
  severities.sort((a, b) => a.rank - b.rank);
  const nameOf = (list: { id: string; name: string }[], fallback: string) => {
    const names = new Map(list.map((x) => [x.id, x.name]));
    return (id: string) => names.get(id) ?? fallback;
  };
  return {
    categories,
    severities,
    sites,
    users,
    categoryName: nameOf(categories, "Unknown category"),
    severity: (id) => severities.find((s) => s.id === id),
    siteName: nameOf(sites, "Unknown site"),
    userName: nameOf(users, "Unknown user"),
  };
}
