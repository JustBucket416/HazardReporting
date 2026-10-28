"use client";

import dynamic from "next/dynamic";
import type { MapProps } from "./incident-map";

// Leaflet touches `window` on import, so the map is loaded in the browser only.
const IncidentMap = dynamic(() => import("./incident-map"), {
  ssr: false,
  loading: () => <div className="h-64 w-full animate-pulse rounded-lg border bg-muted" />,
});

export function MapLoader(props: MapProps) {
  return <IncidentMap {...props} />;
}
