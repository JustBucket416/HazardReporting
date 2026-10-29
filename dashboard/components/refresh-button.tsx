"use client";

import { useRouter } from "next/navigation";
import { useTransition } from "react";
import { RefreshCwIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/** Re-renders the current page's Server Components with fresh data from the server. */
export function RefreshButton() {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  return (
    <Button variant="outline" size="sm" disabled={pending} onClick={() => startTransition(() => router.refresh())}>
      <RefreshCwIcon className={cn(pending && "animate-spin")} />
      Refresh
    </Button>
  );
}
