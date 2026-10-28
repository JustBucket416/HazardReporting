"use server";

import { revalidatePath } from "next/cache";
import { ApiError, apiFetch } from "@/lib/api";
import { isPriority, isStatus } from "@/lib/incidents";

export type ActionState = { error?: string; saved?: boolean; draft?: string; attempt?: number };

/** Operator PATCH (spec § 1): only the fields an operator owns (ADR-0004). */
async function patchIncident(id: string, body: Record<string, unknown>): Promise<string | undefined> {
  try {
    await apiFetch(`/incidents/${encodeURIComponent(id)}`, { method: "PATCH", body: JSON.stringify(body) });
  } catch (e) {
    if (e instanceof ApiError) return e.details; // e.g. the server's 409/400 message
    throw e; // includes the redirect a 401 triggers
  }
  revalidatePath("/incidents", "layout");
  return undefined;
}

/**
 * Saves status, priority and assignee. Only fields that differ from what the
 * form was rendered with are sent, so a stale form can't overwrite a field
 * another operator changed meanwhile.
 */
export async function saveTriage(id: string, prev: ActionState, formData: FormData): Promise<ActionState> {
  const attempt = (prev.attempt ?? 0) + 1;
  const field = (name: string) => String(formData.get(name) ?? "");
  const body: Record<string, unknown> = {};

  const status = field("status");
  if (isStatus(status) && status !== field("initialStatus")) body.status = status;
  const priority = field("priority");
  if (priority !== field("initialPriority")) body.priority = isPriority(priority) ? priority : null;
  const assignee = field("assignee").trim();
  if (assignee !== field("initialAssignee")) body.assignee = assignee || null;

  if (Object.keys(body).length === 0) return { error: "Nothing to save — no changes made.", attempt };
  const error = await patchIncident(id, body);
  return error ? { error, attempt } : { saved: true, attempt };
}

/** Appends a Note; the server stamps it with the operator and the time. */
export async function addNote(id: string, prev: ActionState, formData: FormData): Promise<ActionState> {
  const attempt = (prev.attempt ?? 0) + 1;
  const note = String(formData.get("note") ?? "").trim();
  if (!note) return { error: "Write a note first.", attempt };
  const error = await patchIncident(id, { note });
  return error ? { error, draft: note, attempt } : { saved: true, attempt };
}
