import { ImageResponse } from "next/og";
export const alt = "Klyra — Every song, a little closer. For Android and Windows. Developed by Ryan.";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";
export default function SocialImage() {
  return new ImageResponse(
    <div style={{ width: "100%", height: "100%", background: "#111111", display: "flex", padding: 64, flexDirection: "column", fontFamily: "sans-serif", color: "#eeeeee" }}>
      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 18, color: "#aaaaaa" }}><span>YOUR MUSIC. YOUR SPACE.</span><span>ANDROID + WINDOWS</span></div>
      <div style={{ display: "flex", fontSize: 250, lineHeight: 1.1, letterSpacing: -18, fontWeight: 600, marginTop: 35 }}>Klyra</div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginTop: 55, borderTop: "1px solid #444444", paddingTop: 30 }}><span style={{ fontSize: 32 }}>Every song, a little closer.</span><span style={{ fontSize: 19, color: "#aaaaaa" }}>Developed by Ryan</span></div>
    </div>, size,
  );
}
