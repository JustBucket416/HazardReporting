import { cookies } from "next/headers";
import { TIME_ZONE_COOKIE, USER_COOKIE } from "./config";

export type Operator = { id: string; name: string };

/** The signed-in operator as remembered at login, or null. */
export async function getOperator(): Promise<Operator | null> {
  const raw = (await cookies()).get(USER_COOKIE)?.value;
  if (!raw) return null;
  try {
    const { id, name } = JSON.parse(raw) as Operator;
    return typeof id === "string" && typeof name === "string" ? { id, name } : null;
  } catch {
    return null;
  }
}

/** The operator's time zone (from the login form), or undefined for the server's own. */
export async function getTimeZone(): Promise<string | undefined> {
  const tz = (await cookies()).get(TIME_ZONE_COOKIE)?.value;
  if (!tz) return undefined;
  try {
    new Intl.DateTimeFormat("en", { timeZone: tz });
    return tz;
  } catch {
    return undefined;
  }
}
