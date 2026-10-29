import { NextRequest, NextResponse } from "next/server";
import { createReadStream } from "node:fs";
import { stat } from "node:fs/promises";
import { resolve } from "node:path";
import { Readable } from "node:stream";
import { assets, assetUrl, isAssetKey, release } from "@/lib/release";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

async function respond(request: NextRequest, { params }: { params: Promise<{ asset: string }> }, head = false) {
  const { asset } = await params;
  if (!isAssetKey(asset)) return new NextResponse("Unknown download", { status: 404 });
  const file = assets[asset].file;
  // Local testing only: never upload or proxy large installers through Vercel.
  const local = !process.env.VERCEL && process.env.KLYRA_LOCAL_RELEASE_DIR;
  if (local) {
    const path = resolve(local, file);
    try {
      const info = await stat(path);
      const range = request.headers.get("range");
      const match = range?.match(/^bytes=(\d+)-(\d*)$/);
      const start = match ? Number(match[1]) : 0;
      const end = match?.[2] ? Number(match[2]) : info.size - 1;
      if (range && (!match || start > end || end >= info.size)) return new NextResponse(null, { status: 416, headers: { "Content-Range": `bytes */${info.size}` } });
      const headers: Record<string, string> = {
        "Content-Type": file.endsWith(".apk") ? "application/vnd.android.package-archive" : file.endsWith(".zip") ? "application/zip" : "text/plain; charset=utf-8",
        "Content-Disposition": `attachment; filename="${file}"`,
        "Content-Length": String(end - start + 1), "Accept-Ranges": "bytes", "Cache-Control": "no-store",
      };
      if (match) headers["Content-Range"] = `bytes ${start}-${end}/${info.size}`;
      return new NextResponse(head ? null : Readable.toWeb(createReadStream(path, { start, end })) as ReadableStream, { status: match ? 206 : 200, headers });
    } catch { return new NextResponse("Local release file is missing. See website/README.md.", { status: 503 }); }
  }
  // Confirm the release contains this asset instead of sending visitors to a dead link.
  try {
    const result = await fetch(`https://api.github.com/repos/${release.repository}/releases/tags/${release.tag}`, {
      headers: { Accept: "application/vnd.github+json", "User-Agent": "Klyra-website" },
      next: { revalidate: 300 }, signal: AbortSignal.timeout(8000),
    });
    if (result.ok) {
      const data = await result.json() as { draft?: boolean; assets?: { name: string }[] };
      if (!data.draft && data.assets?.some(item => item.name === file)) return NextResponse.redirect(assetUrl(asset), 307);
    }
  } catch { /* Keep a readable retry page when the release host is unavailable. */ }
  return NextResponse.redirect(new URL(`/download-unavailable?file=${asset}`, request.url), 303);
}
export async function GET(request: NextRequest, context: { params: Promise<{ asset: string }> }) { return respond(request, context); }
export async function HEAD(request: NextRequest, context: { params: Promise<{ asset: string }> }) { return respond(request, context, true); }
