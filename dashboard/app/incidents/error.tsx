"use client";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

/** Shown when the server is unreachable or answers with an unexpected error. */
export default function IncidentsError({ error, retry }: { error: unknown; retry: () => void }) {
  // In production Next.js hides server error messages, so keep a useful default.
  const message = error instanceof Error && error.message ? error.message : "Is the server running?";
  return (
    <Alert variant="destructive" className="max-w-xl">
      <AlertTitle>Couldn&apos;t load incidents</AlertTitle>
      <AlertDescription>
        <p>{message}</p>
        <Button variant="outline" size="sm" onClick={() => retry()}>
          Try again
        </Button>
      </AlertDescription>
    </Alert>
  );
}
