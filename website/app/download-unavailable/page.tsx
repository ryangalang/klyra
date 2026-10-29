import Link from "next/link";
import { Brand } from "@/components/brand";
import { assets, isAssetKey, release } from "@/lib/release";
export const metadata = { title: "Download availability — Klyra", robots: { index: false, follow: true } };
export default async function DownloadUnavailable({ searchParams }: { searchParams: Promise<{ file?: string }> }) {
  const { file } = await searchParams;
  const key = file && isAssetKey(file) ? file : "android";
  return <main className="simple-page"><Link href="/"><Brand/></Link><span className="eyebrow">A SMALL PAUSE</span><h1>Your download isn’t available right now.</h1><p>We couldn’t confirm <strong>{assets[key].label}</strong> in release {release.version}. The file may still be publishing, or the download host may be temporarily unavailable.</p><p>Try again in a moment, or check the release files. You haven’t downloaded or installed anything yet.</p><a className="button button-dark" href={`/download/${key}`}>Try download again</a><p style={{marginTop:24}}><a href={`https://github.com/${release.repository}/releases`}>Check release files</a> · <Link href="/#download">Back to Klyra</Link></p></main>;
}
