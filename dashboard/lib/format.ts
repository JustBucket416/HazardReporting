// Dates are epoch milliseconds on the wire and are shown in the operator's own
// time zone (captured at login; the server's zone is the fallback).

export function formatDateTime(ms: number, timeZone?: string): string {
  return new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeStyle: "short", timeZone }).format(ms);
}

export function formatDate(ms: number, timeZone?: string): string {
  return new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeZone }).format(ms);
}

/** How far `timeZone` is ahead of UTC at the instant `utcMs`, in ms. */
function zoneOffset(utcMs: number, timeZone?: string): number {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    hourCycle: "h23",
    year: "numeric",
    month: "numeric",
    day: "numeric",
    hour: "numeric",
    minute: "numeric",
    second: "numeric",
  }).formatToParts(utcMs);
  const get = (type: Intl.DateTimeFormatPartTypes) => Number(parts.find((p) => p.type === type)?.value);
  const wallClock = Date.UTC(get("year"), get("month") - 1, get("day"), get("hour"), get("minute"), get("second"));
  return wallClock - Math.floor(utcMs / 1000) * 1000;
}

/**
 * The instant local midnight starts on `date` (`YYYY-MM-DD`) in `timeZone`,
 * shifted by `addDays`; undefined for a malformed date.
 */
export function startOfLocalDay(date: string, timeZone?: string, addDays = 0): number | undefined {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  if (!m) return undefined;
  const wallClock = Date.UTC(Number(m[1]), Number(m[2]) - 1, Number(m[3]) + addDays);
  if (Number.isNaN(wallClock)) return undefined;
  const guess = wallClock - zoneOffset(wallClock, timeZone);
  // Second pass corrects for a DST change between the guess and the real instant.
  return wallClock - zoneOffset(guess, timeZone);
}
