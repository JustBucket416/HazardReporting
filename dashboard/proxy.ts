import { NextResponse, type NextRequest } from "next/server";
import { TOKEN_COOKIE } from "@/lib/config";

/**
 * Next.js 16's successor to middleware.ts: an optimistic auth check. Without
 * the token cookie every page redirects to /login; with it, /login goes on to
 * the incidents. Whether the token is still valid is decided by the server (a
 * 401 leads to /session-expired).
 */
export function proxy(request: NextRequest) {
  const signedIn = request.cookies.has(TOKEN_COOKIE);
  const isLogin = request.nextUrl.pathname === "/login";

  if (!signedIn && !isLogin) return NextResponse.redirect(new URL("/login", request.url));
  if (signedIn && isLogin) return NextResponse.redirect(new URL("/incidents", request.url));
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|session-expired).*)"],
};
