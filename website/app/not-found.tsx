import Link from "next/link";
import { Brand } from "@/components/brand";
export default function NotFound(){return <main className="simple-page"><Brand/><span className="eyebrow">404</span><h1>A track off the playlist.</h1><p>That page isn’t here. Your music is a little closer on the home page.</p><Link className="button button-dark" href="/">Back to Klyra</Link></main>;}
