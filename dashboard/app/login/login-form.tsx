"use client";

import { useActionState, useEffect, useRef } from "react";
import { login, type LoginState } from "@/app/auth-actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function LoginForm() {
  const [state, action, pending] = useActionState<LoginState, FormData>(login, {});
  const timeZone = useRef<HTMLInputElement>(null);

  // The operator's time zone travels with the login so the server renders dates in local time.
  useEffect(() => {
    if (timeZone.current) timeZone.current.value = Intl.DateTimeFormat().resolvedOptions().timeZone;
  }, []);

  return (
    <form action={action} className="grid gap-4">
      <input ref={timeZone} type="hidden" name="timeZone" />
      <div className="grid gap-1.5">
        <Label htmlFor="email">Email</Label>
        <Input id="email" name="email" type="email" autoComplete="username" defaultValue={state.email} required />
      </div>
      <div className="grid gap-1.5">
        <Label htmlFor="password">Password</Label>
        <Input id="password" name="password" type="password" autoComplete="current-password" required />
      </div>
      {state.error && (
        <p role="alert" className="text-sm text-destructive">
          {state.error}
        </p>
      )}
      <Button type="submit" disabled={pending}>
        {pending ? "Signing in…" : "Sign in"}
      </Button>
    </form>
  );
}
