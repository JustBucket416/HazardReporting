"use client";

import { useActionState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select";
import { PRIORITIES, PRIORITY_LABELS, STATUS_LABELS, type Priority, type Status } from "@/lib/incidents";
import { saveTriage, type ActionState } from "./actions";

type Props = {
  incidentId: string;
  status: Status;
  /** The statuses the operator may move to from `status`. */
  transitions: readonly Status[];
  priority: Priority | null;
  assignee: string | null;
  /** Changes whenever the incident does, so the fields re-mount with the server's values. */
  version: number;
};

export function TriageForm({ incidentId, status, transitions, priority, assignee, version }: Props) {
  const [state, action, pending] = useActionState<ActionState, FormData>(saveTriage.bind(null, incidentId), {});

  return (
    <form action={action} className="grid gap-4">
      <div key={version} className="grid gap-4">
        <input type="hidden" name="initialStatus" value={status} />
        <input type="hidden" name="initialPriority" value={priority ?? ""} />
        <input type="hidden" name="initialAssignee" value={assignee ?? ""} />
        <div className="grid gap-1.5">
          <Label htmlFor="triage-status">Status</Label>
          <NativeSelect id="triage-status" name="status" defaultValue={status} className="w-full">
            <NativeSelectOption value={status}>{STATUS_LABELS[status]} (current)</NativeSelectOption>
            {transitions.map((s) => (
              <NativeSelectOption key={s} value={s}>
                {STATUS_LABELS[s]}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="triage-priority">Priority</Label>
          <NativeSelect id="triage-priority" name="priority" defaultValue={priority ?? ""} className="w-full">
            <NativeSelectOption value="">Not set</NativeSelectOption>
            {PRIORITIES.map((p) => (
              <NativeSelectOption key={p} value={p}>
                {PRIORITY_LABELS[p]}
              </NativeSelectOption>
            ))}
          </NativeSelect>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="triage-assignee">Assignee</Label>
          <Input id="triage-assignee" name="assignee" placeholder="Crew or team" defaultValue={assignee ?? ""} maxLength={100} />
        </div>
      </div>
      {state.error && (
        <p role="alert" className="text-sm text-destructive">
          {state.error}
        </p>
      )}
      {state.saved && !pending && (
        <p role="status" className="text-sm text-green-700">
          Saved.
        </p>
      )}
      <Button type="submit" disabled={pending} className="justify-self-start">
        {pending ? "Saving…" : "Save triage"}
      </Button>
    </form>
  );
}
