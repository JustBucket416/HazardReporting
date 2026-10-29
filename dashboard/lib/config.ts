/** Base URL of the json-server backend (spec § 1). Override with the API_BASE_URL env var. */
export const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:3001";

/** httpOnly cookie holding the operator's bearer token. */
export const TOKEN_COOKIE = "ir_token";
/** The signed-in operator's `{ id, name }`, for the header. */
export const USER_COOKIE = "ir_user";
/** The operator's IANA time zone, captured by the login form, so dates render in local time. */
export const TIME_ZONE_COOKIE = "ir_tz";

/** Route handler that clears the session cookies and sends the operator to /login. */
export const SESSION_EXPIRED_PATH = "/session-expired";
