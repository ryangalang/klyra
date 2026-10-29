import { ImageResponse } from "next/og";
export const alt = "Klyra — Every song, a little closer. For Android and Windows. Developed by Ryan.";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";
export default function SocialImage(){return new ImageResponse(<div style={{width:"100%",height:"100%",background:"#f0f3e9",display:"flex",padding:70,flexDirection:"column",fontFamily:"sans-serif",color:"#242b21"}}><div style={{display:"flex",fontSize:30,fontWeight:700}}>Klyra</div><div style={{display:"flex",flexDirection:"column",marginTop:70,fontSize:92,lineHeight:1.04,letterSpacing:-5,fontWeight:600}}><span>Every song,</span><span style={{color:"#6b805f"}}>a little closer.</span></div><div style={{display:"flex",justifyContent:"space-between",marginTop:65,fontSize:19,color:"#7a876c"}}><span>For Android & Windows</span><span>Developed by Ryan</span></div></div>,size);}
