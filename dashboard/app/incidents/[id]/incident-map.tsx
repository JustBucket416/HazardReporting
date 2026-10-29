"use client";

import "leaflet/dist/leaflet.css";
import L from "leaflet";
import { useEffect, useRef } from "react";

export type MapProps = { lat: number; lng: number; accuracyM: number };

/** A small OpenStreetMap view of the incident's Position, with its accuracy circle. */
export default function IncidentMap({ lat, lng, accuracyM }: MapProps) {
  const container = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!container.current) return;
    const map = L.map(container.current, { scrollWheelZoom: false }).setView([lat, lng], 17);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    }).addTo(map);
    L.circle([lat, lng], { radius: accuracyM, color: "#2563eb", weight: 1, fillOpacity: 0.15 }).addTo(map);
    // A circle marker avoids Leaflet's default icon images, which bundlers don't resolve.
    L.circleMarker([lat, lng], { radius: 6, color: "#fff", weight: 2, fillColor: "#2563eb", fillOpacity: 1 }).addTo(map);
    return () => {
      map.remove();
    };
  }, [lat, lng, accuracyM]);

  return <div ref={container} className="isolate h-64 w-full rounded-lg border" />;
}
