import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Incident Dashboard",
  description: "Operator dashboard for triaging field incidents",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="flex min-h-full flex-col bg-neutral-50">{children}</body>
    </html>
  );
}
