"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { FormEvent, ReactNode } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select";
import { incidentsHref, parseIncidentQuery, type IncidentQuery } from "@/lib/incident-query";
import {
  PRIORITIES,
  PRIORITY_LABELS,
  STATUSES,
  STATUS_LABELS,
  type Category,
  type Severity,
  type Site,
} from "@/lib/incidents";

type Props = { query: IncidentQuery; categories: Category[]; severities: Severity[]; sites: Site[] };

function Field({ label, htmlFor, children }: { label: string; htmlFor: string; children: ReactNode }) {
  return (
    <div className="grid gap-1">
      <Label htmlFor={htmlFor} className="text-xs text-muted-foreground">
        {label}
      </Label>
      {children}
    </div>
  );
}

function Select({ name, value, options }: { name: string; value?: string; options: { value: string; label: string }[] }) {
  return (
    <NativeSelect id={`filter-${name}`} name={name} defaultValue={value ?? ""} className="w-full">
      <NativeSelectOption value="">All</NativeSelectOption>
      {options.map((o) => (
        <NativeSelectOption key={o.value} value={o.value}>
          {o.label}
        </NativeSelectOption>
      ))}
    </NativeSelect>
  );
}

/** Filters for the incident table; applying them rewrites the URL (and goes back to page 1). */
export function FilterBar({ query, categories, severities, sites }: Props) {
  const router = useRouter();

  function apply(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = Object.fromEntries(new FormData(e.currentTarget)) as Record<string, string>;
    const filters = parseIncidentQuery(form);
    router.push(incidentsHref({ ...filters, sort: query.sort, order: query.order, limit: query.limit, page: 1 }));
  }

  return (
    // Keyed by the URL state so the inputs follow back/forward navigation and "Clear".
    <form key={incidentsHref(query)} onSubmit={apply} className="rounded-lg border bg-background p-4">
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-8">
        <div className="col-span-2">
          <Field label="Search" htmlFor="filter-q">
            <Input id="filter-q" name="q" type="search" placeholder="Title, description…" defaultValue={query.q} />
          </Field>
        </div>
        <Field label="Status" htmlFor="filter-status">
          <Select name="status" value={query.status} options={STATUSES.map((s) => ({ value: s, label: STATUS_LABELS[s] }))} />
        </Field>
        <Field label="Severity" htmlFor="filter-severity">
          <Select name="severity" value={query.severity} options={severities.map((s) => ({ value: s.id, label: s.name }))} />
        </Field>
        <Field label="Priority" htmlFor="filter-priority">
          <Select
            name="priority"
            value={query.priority}
            options={PRIORITIES.map((p) => ({ value: p, label: PRIORITY_LABELS[p] }))}
          />
        </Field>
        <Field label="Category" htmlFor="filter-category">
          <Select name="category" value={query.category} options={categories.map((c) => ({ value: c.id, label: c.name }))} />
        </Field>
        <Field label="Site" htmlFor="filter-site">
          <Select name="site" value={query.site} options={sites.map((s) => ({ value: s.id, label: s.name }))} />
        </Field>
        <Field label="Assignee" htmlFor="filter-assignee">
          <Input id="filter-assignee" name="assignee" placeholder="Any" defaultValue={query.assignee} />
        </Field>
        <Field label="Created from" htmlFor="filter-from">
          <Input id="filter-from" name="from" type="date" defaultValue={query.from} />
        </Field>
        <Field label="Created to" htmlFor="filter-to">
          <Input id="filter-to" name="to" type="date" defaultValue={query.to} />
        </Field>
        <div className="col-span-2 flex items-end gap-2">
          <Button type="submit">Apply filters</Button>
          <Link href="/incidents" className={buttonVariants({ variant: "ghost" })}>
            Clear
          </Link>
        </div>
      </div>
    </form>
  );
}
