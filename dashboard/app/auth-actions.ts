"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { API_BASE_URL, TIME_ZONE_COOKIE, TOKEN_COOKIE, USER_COOKIE } from "@/lib/config";

export type LoginState = { error?: string; email?: string };

type LoginResponse = { token: string; user: { id: string; email: string; name: string; role: string } };

const COOKIE_OPTIONS = { httpOnly: true, sameSite: "lax", path: "/" } as const;

async function postLogout(token: string) {
  await fetch(`${API_BASE_URL}/logout`, { method: "POST", headers: { Authorization: `Bearer ${token}` } }).catch(
    () => undefined,
  );
}

/** Signs in through the server's POST /login; only operators get a session cookie. */
export async function login(_prev: LoginState, formData: FormData): Promise<LoginState> {
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");
  const timeZone = String(formData.get("timeZone") ?? "");
  if (!email || !password) return { error: "Enter your email and password", email };

  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
  } catch {
    return { error: `Can't reach the server at ${API_BASE_URL}`, email };
  }
  if (res.status === 401) return { error: "Invalid email or password", email };
  if (!res.ok) return { error: `Sign-in failed (${res.status})`, email };

  const { token, user } = (await res.json()) as LoginResponse;
  if (user.role !== "operator") {
    // Don't leave a technician session behind on the server.
    await postLogout(token);
    return { error: "Operators only", email };
  }

  const jar = await cookies();
  jar.set(TOKEN_COOKIE, token, COOKIE_OPTIONS);
  jar.set(USER_COOKIE, JSON.stringify({ id: user.id, name: user.name }), COOKIE_OPTIONS);
  if (timeZone) jar.set(TIME_ZONE_COOKIE, timeZone, COOKIE_OPTIONS);
  redirect("/incidents");
}

/** Ends the session on the server (best effort) and clears the cookies. */
export async function logout() {
  const jar = await cookies();
  const token = jar.get(TOKEN_COOKIE)?.value;
  if (token) await postLogout(token);
  jar.delete(TOKEN_COOKIE);
  jar.delete(USER_COOKIE);
  redirect("/login");
}
