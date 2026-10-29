export function Mark({ className = "" }: { className?: string }) {
  return <svg className={className} viewBox="0 0 320 320" fill="currentColor" aria-hidden="true"><path transform="translate(28 9.5)" d="M0 35 L71 0 L71 137 C108 68 199 51 251 9 L251 52 C251 95 211 109 146 127 C105 137 78 153 71 172 L71 185 C95 152 125 151 156 167 C192 187 233 210 264 211 L264 284 C213 290 183 256 154 230 C113 190 91 187 71 205 L71 301 L0 266 Z"/></svg>;
}
export function Brand() { return <span className="brand"><Mark />Klyra</span>; }
