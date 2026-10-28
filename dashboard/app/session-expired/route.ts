import { NextResponse, type NextRequest } from "next/server";
import { TOKEN_COOKIE, USER_COOKIE } from "@/lib/config";

/**
 * Where data fetches go when the server answers 401 (e.g. after `npm run reset`):
 * Server Components can't change cookies, so this handler clears them and sends
 * the operator back to /login.
 */
export function GET(request: NextRequest) {
  const response = NextResponse.redirect(new URL("/login", request.url));
  response.cookies.delete(TOKEN_COOKIE);
  response.cookies.delete(USER_COOKIE);
  return response;
}
