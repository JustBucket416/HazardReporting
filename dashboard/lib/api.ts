import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { API_BASE_URL, SESSION_EXPIRED_PATH, TOKEN_COOKIE } from "./config";

/** A non-2xx answer from the server, carrying its `{ error, fields? }` body (spec § 1 "Error body"). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly fields?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
  }

  /** The server's message plus any per-field messages, for showing to the operator. */
  get details(): string {
    const fields = Object.entries(this.fields ?? {}).map(([field, msg]) => `${field}: ${msg}`);
    return [this.message, ...fields].join(" — ");
  }
}

async function toApiError(res: Response): Promise<ApiError> {
  const body = (await res.json().catch(() => null)) as { error?: string; fields?: Record<string, string> } | null;
  return new ApiError(res.status, body?.error ?? `Request failed (${res.status})`, body?.fields);
}

/**
 * Calls the server as the signed-in operator: reads the token from the httpOnly
 * cookie and sends it as a Bearer header. A missing token redirects to /login;
 * a 401 clears the cookie and redirects there too. Other failures throw ApiError.
 */
export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = (await cookies()).get(TOKEN_COOKIE)?.value;
  if (!token) redirect("/login");

  let res: Response;
  try {
    res = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init.body ? { "Content-Type": "application/json" } : {}),
        Authorization: `Bearer ${token}`,
        ...init.headers,
      },
    });
  } catch {
    throw new ApiError(0, `Can't reach the server at ${API_BASE_URL}`);
  }
  if (res.status === 401) redirect(SESSION_EXPIRED_PATH);
  if (!res.ok) throw await toApiError(res);
  return res;
}

export async function apiGet<T>(path: string): Promise<T> {
  return (await apiFetch(path)).json() as Promise<T>;
}

/** The absolute URL of a server-relative path such as a photo's `/uploads/…`. */
export function serverUrl(path: string): string {
  return new URL(path, API_BASE_URL).toString();
}
