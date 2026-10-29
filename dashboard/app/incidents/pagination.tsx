import Link from "next/link";
import type { ReactNode } from "react";
import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { incidentsHref, PAGE_SIZES, type IncidentQuery } from "@/lib/incident-query";
import { cn } from "@/lib/utils";

/** Page links driven by the server's X-Total-Count. */
export function Pagination({ query, total }: { query: IncidentQuery; total: number }) {
  const pages = Math.max(1, Math.ceil(total / query.limit));
  const first = total === 0 ? 0 : (query.page - 1) * query.limit + 1;
  const last = Math.min(total, query.page * query.limit);
  const pageLink = (page: number, label: string, icon: ReactNode, disabled: boolean) =>
    disabled ? (
      <span className={cn(buttonVariants({ variant: "outline", size: "sm" }), "pointer-events-none opacity-50")} aria-disabled>
        {icon}
        {label}
      </span>
    ) : (
      <Link href={incidentsHref({ ...query, page })} className={buttonVariants({ variant: "outline", size: "sm" })}>
        {icon}
        {label}
      </Link>
    );

  return (
    <nav aria-label="Pagination" className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <p className="text-muted-foreground">
        {total === 0 ? "No incidents" : `Showing ${first}–${last} of ${total}`}
        <span className="ml-4">
          Rows per page:{" "}
          {PAGE_SIZES.map((size) => (
            <Link
              key={size}
              href={incidentsHref({ ...query, limit: size, page: 1 })}
              className={cn("px-1", size === query.limit ? "font-semibold text-foreground" : "hover:underline")}
              aria-current={size === query.limit ? "true" : undefined}
            >
              {size}
            </Link>
          ))}
        </span>
      </p>
      <div className="flex items-center gap-2">
        {pageLink(query.page - 1, "Previous", <ChevronLeftIcon />, query.page <= 1)}
        <span className="text-muted-foreground">
          Page {Math.min(query.page, pages)} of {pages}
        </span>
        {pageLink(query.page + 1, "Next", <ChevronRightIcon />, query.page >= pages)}
      </div>
    </nav>
  );
}
