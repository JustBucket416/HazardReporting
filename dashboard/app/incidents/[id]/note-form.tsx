"use client";

import { useActionState } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { addNote, type ActionState } from "./actions";

export function NoteForm({ incidentId }: { incidentId: string }) {
  const [state, action, pending] = useActionState<ActionState, FormData>(addNote.bind(null, incidentId), {});

  return (
    <form action={action} className="grid gap-2">
      <Label htmlFor="note" className="sr-only">
        New note
      </Label>
      {/* Re-mounted per attempt: empty after a save, keeps the text after a failure. */}
      <Textarea
        key={state.attempt}
        id="note"
        name="note"
        rows={3}
        placeholder="Internal note for operators…"
        defaultValue={state.draft}
        required
      />
      {state.error && (
        <p role="alert" className="text-sm text-destructive">
          {state.error}
        </p>
      )}
      <Button type="submit" variant="outline" disabled={pending} className="justify-self-start">
        {pending ? "Adding…" : "Add note"}
      </Button>
    </form>
  );
}
