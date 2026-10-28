import Link from "next/link";
import { Suspense } from "react";
import { LogOutIcon } from "lucide-react";
import { logout } from "@/app/auth-actions";
import { RefreshButton } from "@/components/refresh-button";
import { Button } from "@/components/ui/button";
import { getOperator } from "@/lib/session";

async function OperatorName() {
  const operator = await getOperator();
  return operator ? <span className="hidden text-sm text-muted-foreground sm:inline">{operator.name}</span> : null;
}

export default function IncidentsLayout({ children }: LayoutProps<"/incidents">) {
  return (
    <>
      <header className="sticky top-0 z-10 border-b bg-background">
        <div className="mx-auto flex h-14 max-w-7xl items-center gap-3 px-4">
          <Link href="/incidents" className="font-semibold">
            Incident Dashboard
          </Link>
          <div className="ml-auto flex items-center gap-2">
            <Suspense>
              <OperatorName />
            </Suspense>
            <RefreshButton />
            <form action={logout}>
              <Button type="submit" variant="ghost" size="sm">
                <LogOutIcon />
                Log out
              </Button>
            </form>
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6">{children}</main>
    </>
  );
}
