"use client";

import { useEffect, useRef, useState } from "react";
import { ArrowUpRight, AudioLines, ChevronLeft, ChevronRight, Pause, Play, RotateCcw } from "lucide-react";
import songs from "@/lib/catalog.json";

export const keepMe = songs[0];

// Original illustration text, not lyrics attributed to Novo Amor.
const demoLines = ["Let the city fade away", "We take the\nlong way home.", "A little closer\nwith every song.", "Let the city fade away", "We take the\nlong way home.", "A little closer\nwith every song."];
const demoDuration = 24;

export function LyricMotionPreview() {
  const card = useRef<HTMLDivElement>(null);
  const positionRef = useRef(4);
  const [position, setPosition] = useState(4);
  const [playing, setPlaying] = useState(true);
  const [visible, setVisible] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(false);
  useEffect(() => {
    const preference = matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => { setReducedMotion(preference.matches); if (preference.matches) setPlaying(false); };
    update(); preference.addEventListener("change", update);
    const observer = new IntersectionObserver(entries => setVisible(entries[0].isIntersecting), { threshold: 0.2 });
    if (card.current) observer.observe(card.current);
    return () => { observer.disconnect(); preference.removeEventListener("change", update); };
  }, []);
  useEffect(() => {
    if (!playing || !visible) return;
    let frame = 0;
    let previous = performance.now();
    let lastRender = previous;
    const tick = (now: number) => {
      // Pause elapsed time when the document is hidden; never jump after switching tabs.
      if (!document.hidden) positionRef.current = (positionRef.current + Math.min((now - previous) / 1000, 0.25)) % demoDuration;
      previous = now;
      if (now - lastRender >= 80) { setPosition(positionRef.current); lastRender = now; }
      frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [playing, visible]);
  function seek(value: number) { positionRef.current = value; setPosition(value); }
  const activeLine = Math.min(demoLines.length - 1, Math.floor(position / 4));
  return <div className={`lyric-motion-card${reducedMotion ? " reduce-motion" : ""}`} ref={card} aria-label="Silent animated lyrics preview using original demo text">
    <img className="lyric-motion-backdrop" src="/art/afterglow.svg" alt=""/>
    <div className="lyric-motion-header"><img src="/art/afterglow.svg" alt="" width="52" height="52"/><div><strong>Closer to the light</strong><span>Klyra Sessions · motion preview</span></div><AudioLines size={25}/></div>
    <div className="motion-lines" aria-label="Sample lyric lines">{demoLines.map((line,index) => {
      const distance=index-activeLine;
      return <button key={index} className={`motion-line${distance===0?" is-current":""}`} aria-current={distance===0?"true":undefined} tabIndex={Math.abs(distance)<=1?0:-1} aria-hidden={Math.abs(distance)>1} onClick={()=>seek(index*4)} style={{transform:`translateY(${distance*112}px) scale(${distance===0?1:.94})`,opacity:distance===0?1:Math.abs(distance)===1?.34:0}}>{line}</button>;
    })}</div>
    <div className="motion-controls"><button onClick={()=>setPlaying(value=>!value)} aria-label={playing?"Pause lyric animation":"Play lyric animation"}>{playing?<Pause size={17} fill="currentColor"/>:<Play size={17} fill="currentColor"/>}</button><span>0:{String(Math.floor(position)).padStart(2,"0")}</span><input aria-label="Lyric animation position" type="range" min="0" max="23.99" step="0.1" value={position} onChange={event=>seek(Number(event.target.value))}/><span>0:24</span><button onClick={()=>seek(0)} aria-label="Restart lyric animation"><RotateCcw size={16}/></button></div>
    <div className="motion-caption"><span>THE WORDS. THE FEELING. TOGETHER.</span><small>Silent motion demo · original sample text</small></div>
  </div>;
}

const integrations = [
  ["youtubemusic", "YouTube Music", "Unofficial catalog & playback"],
  ["spotify", "Spotify", "Library import & matching"],
  ["discord", "Discord", "Listening activity"],
  ["lastdotfm", "Last.fm", "Scrobbling · setup required"],
  ["googlecast", "Google Cast", "Android casting"],
  ["androidauto", "Android Auto", "Car media controls"],
];

export function IntegrationRail() {
  return <section className="integration-section section-width" aria-label="Music services and integrations"><p>Connected to the way you listen.</p><div className="integration-rail">{integrations.map(([id, name, role])=><div className="integration-item" key={id}><div><img src={`/services/${id}.svg`} width="25" height="25" alt=""/><strong>{name}</strong></div><span>{role}</span></div>)}</div><small>Also in Klyra: LRCLIB and Musixmatch lyrics · ListenBrainz listening history · Supabase account sync.<br/>Optional integrations vary by platform and configuration. Service names and marks belong to their owners; no affiliation or endorsement.</small></section>;
}

export function MusicShelves() {
  const albums = useRef<HTMLDivElement>(null);
  const [albumPage, setAlbumPage] = useState(0);
  function move(direction: number) {
    const row = albums.current;
    if (!row) return;
    row.scrollBy({ left: direction * row.clientWidth * 0.8, behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth" });
  }
  return <section className="music-shelves" id="music"><div className="section-width">
    <div className="shelf-intro"><span className="eyebrow">02 — IN GOOD COMPANY</span><h2>Music you know.<br/>More to fall for.</h2><p>A few familiar names to get you started.<br/>OPM, indie, pop, and the songs that stay.</p></div>
    <div className="shelf-heading"><h3>On repeat</h3><span>Selected tracks</span></div>
    <div className="song-columns">{songs.map((song, index)=><a className="song-row" href={song.url} target="_blank" rel="noreferrer" key={song.id} aria-label={`${song.title} by ${song.artist} — view on Apple Music`}><span className="song-number">{String(index+1).padStart(2,"0")}</span><img src={song.artwork} alt="" width="56" height="56" loading="lazy"/><span className="song-label"><strong>{song.title}</strong><small>{song.artist}</small></span><ArrowUpRight size={16}/></a>)}</div>
    <div className="shelf-heading album-heading"><h3>Stay for the whole record.</h3><div className="shelf-arrows"><button aria-label="Previous albums" onClick={()=>move(-1)} disabled={albumPage === 0}><ChevronLeft/></button><button aria-label="Next albums" onClick={()=>move(1)} disabled={albumPage === 2}><ChevronRight/></button></div></div>
    <div className="record-row" ref={albums} onScroll={event=>{const row=event.currentTarget;setAlbumPage(row.scrollLeft<4?0:row.scrollLeft+row.clientWidth>=row.scrollWidth-4?2:1);}}>{songs.map(song=><a className="record-card" key={song.id} href={song.url} target="_blank" rel="noreferrer" aria-label={`${song.album} by ${song.artist} — view on Apple Music`}><img src={song.artwork} alt={`${song.album} cover`} width="240" height="240" loading="lazy"/><strong>{song.album}</strong><span>{song.artist}</span></a>)}</div>
    <p className="catalog-credit">Artwork and metadata via Apple’s catalog. Links open Apple Music; this is an editorial selection, not a live chart or a guarantee of Klyra playback availability.</p>
  </div></section>;
}
