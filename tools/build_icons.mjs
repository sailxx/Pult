import fs from 'node:fs';
import path from 'node:path';
import { execSync } from 'node:child_process';

const chrome = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";

function svgGlyph(size) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 32 32" fill="none">
  <rect x="4.5" y="3.5" width="6.5" height="25" rx="3.25" fill="#FFFFFF"/>
  <path d="M 9.5,3.5 H 19.5 C 25.3,3.5 30,8.2 30,14 C 30,19.8 25.3,24.5 19.5,24.5 H 9.5 Z" fill="#141923" stroke="#FFFFFF" stroke-opacity="0.3" stroke-width="1.8"/>
  <circle cx="19.5" cy="14" r="4.2" fill="#38BDF8"/>
  <circle cx="19.5" cy="14" r="1.8" fill="#FFFFFF"/>
</svg>`;
}

function svgTile(size) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 512 512" fill="none">
  <defs>
    <linearGradient id="bgGrad" x1="256" y1="0" x2="256" y2="512" gradientUnits="userSpaceOnUse">
      <stop stop-color="#141822"/>
      <stop offset="1" stop-color="#080A0E"/>
    </linearGradient>
    <linearGradient id="specular" x1="256" y1="0" x2="256" y2="220" gradientUnits="userSpaceOnUse">
      <stop stop-color="#ffffff" stop-opacity="0.14"/>
      <stop offset="1" stop-color="#ffffff" stop-opacity="0"/>
    </linearGradient>
    <radialGradient id="beaconGlow" cx="50%" cy="50%" r="50%">
      <stop stop-color="#38BDF8" stop-opacity="0.4"/>
      <stop offset="1" stop-color="#38BDF8" stop-opacity="0"/>
    </radialGradient>
    <filter id="blurFilter" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="30"/>
    </filter>
  </defs>
  <rect x="24" y="32" width="464" height="464" rx="114" fill="#000000" opacity="0.6" filter="url(#blurFilter)"/>
  <rect x="20" y="20" width="472" height="472" rx="112" fill="url(#bgGrad)"/>
  <rect x="21" y="21" width="470" height="470" rx="111" stroke="rgba(255,255,255,0.14)" stroke-width="3" fill="none"/>
  <rect x="22" y="22" width="468" height="220" rx="110" fill="url(#specular)"/>
  <circle cx="305" cy="225" r="140" fill="url(#beaconGlow)"/>
  <rect x="150" y="115" width="60" height="270" rx="30" fill="#FFFFFF"/>
  <rect x="151.5" y="116.5" width="57" height="267" rx="28.5" stroke="rgba(0,0,0,0.12)" stroke-width="2" fill="none"/>
  <path d="M 190,115 H 300 C 360,115 408,163 408,223 C 408,283 360,331 300,331 H 190 Z" fill="#151A26" fill-opacity="0.9" stroke="rgba(255,255,255,0.28)" stroke-width="8"/>
  <path d="M 192,123 H 298 C 354,123 398,167 398,223" stroke="rgba(255,255,255,0.2)" stroke-width="3" fill="none"/>
  <circle cx="295" cy="223" r="52" fill="#0D1017" stroke="rgba(255,255,255,0.1)" stroke-width="3"/>
  <circle cx="295" cy="223" r="32" fill="#38BDF8"/>
  <circle cx="295" cy="223" r="14" fill="#FFFFFF"/>
  <circle cx="295" cy="223" r="44" stroke="#38BDF8" stroke-opacity="0.3" stroke-width="4" fill="none"/>
  <rect x="250" y="365" width="130" height="20" rx="10" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.16)" stroke-width="2"/>
  <circle cx="270" cy="375" r="4" fill="#38BDF8"/>
</svg>`;
}

function buildIco(pngEntries) {
  const count = pngEntries.length;
  const headerSize = 6;
  const dirEntrySize = 16;
  const dirSize = count * dirEntrySize;
  let offset = headerSize + dirSize;

  const header = Buffer.alloc(headerSize);
  header.writeUInt16LE(0, 0);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(count, 4);

  const entries = [];
  for (const img of pngEntries) {
    const entry = Buffer.alloc(dirEntrySize);
    entry.writeUInt8(img.width >= 256 ? 0 : img.width, 0);
    entry.writeUInt8(img.height >= 256 ? 0 : img.height, 1);
    entry.writeUInt8(0, 2);
    entry.writeUInt8(0, 3);
    entry.writeUInt16LE(1, 4);
    entry.writeUInt16LE(32, 6);
    entry.writeUInt32LE(img.buffer.length, 8);
    entry.writeUInt32LE(offset, 12);
    entries.push(entry);
    offset += img.buffer.length;
  }

  return Buffer.concat([header, ...entries, ...pngEntries.map(p => p.buffer)]);
}

export { svgGlyph, svgTile, buildIco };
