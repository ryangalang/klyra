import type { Metadata, Viewport } from "next";
import "./globals.css";
import "./redesign.css";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL || (process.env.VERCEL_PROJECT_PRODUCTION_URL ? `https://${process.env.VERCEL_PROJECT_PRODUCTION_URL}` : process.env.VERCEL_URL ? `https://${process.env.VERCEL_URL}` : "http://localhost:3000");
export const metadata: Metadata = {
  ...(siteUrl ? { metadataBase: new URL(siteUrl) } : {}),
  title: "Klyra — Every song, a little closer.",
  description: "A thoughtfully crafted music experience for Android and Windows. Discover music, follow live lyrics, and make your library yours. Developed by Ryan.",
  applicationName: "Klyra",
  icons: { icon: "/icon.svg", apple: "/app-icon.png" },
  openGraph: { title: "Klyra — Every song, a little closer.", description: "Music, beautifully personal. For Android and Windows.", type: "website", images: [{ url: "/opengraph-image", width: 1200, height: 630 }] },
  twitter: { card: "summary_large_image", title: "Klyra", description: "Every song, a little closer.", images: ["/opengraph-image"] },
};
export const viewport: Viewport = { themeColor: "#111111", width: "device-width", initialScale: 1 };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body>{children}</body></html>;
}
