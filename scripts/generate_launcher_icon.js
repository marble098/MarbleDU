#!/usr/bin/env node
/**
 * MarbleDU launcher icon generator.
 *
 * One source of truth for the icon geometry. Emits:
 *   - adaptive icon layers (VectorDrawable): background / foreground / monochrome
 *   - legacy bitmap fallbacks for every density (square + round)
 *   - branding/ic_launcher-512.png (store listing) and SVG sources
 *
 * Usage:  npm i --no-save @resvg/resvg-js sharp && node scripts/generate_launcher_icon.js
 */
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');
const RES = path.join(ROOT, 'app/src/main/res');

const C = 54;            // centre of the 108dp adaptive canvas
const R = 24.5;          // gauge radius (outer edge 28.5 < 33dp safe-zone radius)
const SW = 7.5;          // gauge stroke
const START = 135, SWEEP = 270, PROGRESS = 0.70;
const f = (n) => (Math.round(n * 100) / 100).toString();
const pt = (deg, r = R) => [C + r * Math.cos((deg * Math.PI) / 180), C + r * Math.sin((deg * Math.PI) / 180)];
function arc(a0, a1, r = R) {
  const [x0, y0] = pt(a0, r), [x1, y1] = pt(a1, r);
  const large = Math.abs(a1 - a0) > 180 ? 1 : 0;
  return `M${f(x0)},${f(y0)} A${f(r)},${f(r)} 0 ${large} 1 ${f(x1)},${f(y1)}`;
}
const circle = (cx, cy, r) =>
  `M${f(cx - r)},${f(cy)} a${f(r)},${f(r)} 0 1 0 ${f(2 * r)},0 a${f(r)},${f(r)} 0 1 0 ${f(-2 * r)},0 Z`;

const END = START + SWEEP * PROGRESS;
const track = arc(START, START + SWEEP);
const progress = arc(START, END);
const [kx, ky] = pt(END);
// fine tick marks inside the gauge
const ticks = [];
for (let i = 0; i <= 9; i++) {
  const a = START + (SWEEP / 9) * i;
  const [x0, y0] = pt(a, R - SW / 2 - 1.6), [x1, y1] = pt(a, R - SW / 2 - 3.4);
  ticks.push(`M${f(x0)},${f(y0)} L${f(x1)},${f(y1)}`);
}
const tickPath = ticks.join(' ');
// arrows (download ↓ / upload ↑)
const down = 'M46.6,45.2 L46.6,63.6 M40.4,57.4 L46.6,63.6 L52.8,57.4';
const up = 'M61.4,62.8 L61.4,44.4 M55.2,50.6 L61.4,44.4 L67.6,50.6';
const rings = [38, 47, 57].map((r) => circle(C, C, r));
const wave = 'M0,78 C22,66 40,92 62,82 C80,74 94,80 108,72 L108,108 L0,108 Z';
const sheen = 'M0,0 L108,0 L108,30 C78,44 34,34 0,52 Z';

const col = {
  bg0: '#1A1F6B', bg1: '#2B2FBF', bg2: '#0B1036',
  glow: '#7C8CFF', p0: '#34F5C5', p1: '#22D3EE', p2: '#60A5FA',
  white: '#FFFFFF', up: '#9BE9FF',
};

// ------------------------------------------------------------------ SVG
const defs = `
<defs>
  <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="${col.bg1}"/><stop offset=".55" stop-color="${col.bg0}"/><stop offset="1" stop-color="${col.bg2}"/>
  </linearGradient>
  <radialGradient id="glow" cx="30" cy="22" r="62" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="${col.glow}" stop-opacity=".75"/><stop offset="1" stop-color="${col.glow}" stop-opacity="0"/>
  </radialGradient>
  <radialGradient id="halo" cx="54" cy="54" r="34" gradientUnits="userSpaceOnUse">
    <stop offset=".55" stop-color="${col.p1}" stop-opacity="0"/><stop offset=".8" stop-color="${col.p1}" stop-opacity=".22"/><stop offset="1" stop-color="${col.p1}" stop-opacity="0"/>
  </radialGradient>
  <linearGradient id="pg" x1="28" y1="74" x2="80" y2="34" gradientUnits="userSpaceOnUse">
    <stop offset="0" stop-color="${col.p0}"/><stop offset=".55" stop-color="${col.p1}"/><stop offset="1" stop-color="${col.p2}"/>
  </linearGradient>
</defs>`;
const bgSvg = `
  <rect width="108" height="108" fill="url(#bg)"/>
  <rect width="108" height="108" fill="url(#glow)"/>
  <path d="${sheen}" fill="#FFFFFF" fill-opacity=".05"/>
  ${rings.map((d) => `<path d="${d}" fill="none" stroke="#FFFFFF" stroke-opacity=".07" stroke-width=".8"/>`).join('')}
  <path d="${wave}" fill="#05081F" fill-opacity=".38"/>`;
const fgSvg = `
  <path d="${circle(C, C, 34)}" fill="url(#halo)"/>
  <path d="${arc(START, START + SWEEP)}" transform="translate(0,1.6)" fill="none" stroke="#04061A" stroke-opacity=".35" stroke-width="${SW}" stroke-linecap="round"/>
  <path d="${track}" fill="none" stroke="#FFFFFF" stroke-opacity=".17" stroke-width="${SW}" stroke-linecap="round"/>
  <path d="${tickPath}" fill="none" stroke="#FFFFFF" stroke-opacity=".38" stroke-width="1" stroke-linecap="round"/>
  <path d="${progress}" fill="none" stroke="url(#pg)" stroke-width="${SW}" stroke-linecap="round"/>
  <path d="${arc(START + 6, END - 8, R + 1.9)}" fill="none" stroke="#FFFFFF" stroke-opacity=".35" stroke-width=".9" stroke-linecap="round"/>
  <path d="${circle(kx, ky, 6.6)}" fill="${col.p2}" fill-opacity=".35"/>
  <path d="${circle(kx, ky, 4.7)}" fill="#FFFFFF"/>
  <path d="${circle(kx, ky, 2.1)}" fill="${col.p1}"/>
  <path d="${down}" transform="translate(0,1.2)" fill="none" stroke="#04061A" stroke-opacity=".3" stroke-width="5.2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="${up}" transform="translate(0,1.2)" fill="none" stroke="#04061A" stroke-opacity=".3" stroke-width="5.2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="${down}" fill="none" stroke="${col.white}" stroke-width="5.2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="${up}" fill="none" stroke="${col.p0}" stroke-width="5.2" stroke-linecap="round" stroke-linejoin="round"/>`;
const svg = (inner, extra = '') =>
  `<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">${defs}${extra}${inner}</svg>`;

// -------------------------------------------------------- VectorDrawable
const head = `<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/generate_launcher_icon.js – do not edit by hand. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">`;
const argb = (hex, a = 1) => '#' + Math.round(a * 255).toString(16).padStart(2, '0').toUpperCase() + hex.slice(1);
const grad = (attr, type, geom, stops) => `
        <aapt:attr name="android:${attr}">
            <gradient android:type="${type}" ${geom}>
${stops.map(([o, c]) => `                <item android:offset="${o}" android:color="${c}" />`).join('\n')}
            </gradient>
        </aapt:attr>`;
const lin = (x0, y0, x1, y1) => `android:startX="${x0}" android:startY="${y0}" android:endX="${x1}" android:endY="${y1}"`;
const rad = (cx, cy, r) => `android:centerX="${cx}" android:centerY="${cy}" android:gradientRadius="${r}"`;
const fill = (d, color) => `
    <path android:pathData="${d}" android:fillColor="${color}" />`;
const stroke = (d, color, w, extra = '') => `
    <path android:pathData="${d}" android:strokeColor="${color}" android:strokeWidth="${w}" android:strokeLineCap="round" android:strokeLineJoin="round"${extra} />`;
const group = (ty, inner) => `
    <group android:translateY="${ty}">${inner}
    </group>`;

const full = 'M0,0h108v108h-108z';
const bgXml = `${head}
    <path android:pathData="${full}">${grad('fillColor', 'linear', lin(0, 0, 108, 108), [[0, argb(col.bg1)], [0.55, argb(col.bg0)], [1, argb(col.bg2)]])}
    </path>
    <path android:pathData="${full}">${grad('fillColor', 'radial', rad(30, 22, 62), [[0, argb(col.glow, 0.75)], [1, argb(col.glow, 0)]])}
    </path>${fill(sheen, argb('#FFFFFF', 0.05))}${rings.map((d) => stroke(d, argb('#FFFFFF', 0.07), 0.8)).join('')}${fill(wave, argb('#05081F', 0.38))}
</vector>
`;
const fgXml = `${head}
    <path android:pathData="${circle(C, C, 34)}">${grad('fillColor', 'radial', rad(54, 54, 34), [[0.55, argb(col.p1, 0)], [0.8, argb(col.p1, 0.22)], [1, argb(col.p1, 0)]])}
    </path>${group(1.6, stroke(track, argb('#04061A', 0.35), SW))}${stroke(track, argb('#FFFFFF', 0.17), SW)}${stroke(tickPath, argb('#FFFFFF', 0.38), 1)}
    <path android:pathData="${progress}" android:strokeWidth="${SW}" android:strokeLineCap="round">${grad('strokeColor', 'linear', lin(28, 74, 80, 34), [[0, argb(col.p0)], [0.55, argb(col.p1)], [1, argb(col.p2)]])}
    </path>${stroke(arc(START + 6, END - 8, R + 1.9), argb('#FFFFFF', 0.35), 0.9)}${fill(circle(kx, ky, 6.6), argb(col.p2, 0.35))}${fill(circle(kx, ky, 4.7), '#FFFFFFFF')}${fill(circle(kx, ky, 2.1), argb(col.p1))}${group(1.2, stroke(down, argb('#04061A', 0.3), 5.2) + stroke(up, argb('#04061A', 0.3), 5.2))}${stroke(down, '#FFFFFFFF', 5.2)}${stroke(up, argb(col.p0), 5.2)}
</vector>
`;
// Themed (Material You) layer: only alpha matters, the launcher tints it.
const monoTrack = arc(END + 24, START + SWEEP);
const monoXml = `${head}${stroke(monoTrack, argb('#000000', 0.32), SW)}${stroke(progress, '#FF000000', SW)}${fill(circle(kx, ky, 4.7), '#FF000000')}${stroke(down, '#FF000000', 5.2)}${stroke(up, '#FF000000', 5.2)}
</vector>
`;
const monoSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">
  <path d="${monoTrack}" fill="none" stroke="#000" stroke-opacity=".32" stroke-width="${SW}" stroke-linecap="round"/>
  <path d="${progress}" fill="none" stroke="#000" stroke-width="${SW}" stroke-linecap="round"/>
  <path d="${circle(kx, ky, 4.7)}" fill="#000"/>
  <path d="${down}" fill="none" stroke="#000" stroke-width="5.2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="${up}" fill="none" stroke="#000" stroke-width="5.2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

fs.writeFileSync(path.join(RES, 'drawable/ic_launcher_background.xml'), bgXml);
fs.writeFileSync(path.join(RES, 'drawable/ic_launcher_foreground.xml'), fgXml);
fs.writeFileSync(path.join(RES, 'drawable/ic_launcher_monochrome.xml'), monoXml);
fs.mkdirSync(path.join(ROOT, 'branding'), { recursive: true });
fs.writeFileSync(path.join(ROOT, 'branding/ic_launcher_full.svg'), svg(bgSvg + fgSvg));
fs.writeFileSync(path.join(ROOT, 'branding/ic_launcher_foreground.svg'), svg(fgSvg));
fs.writeFileSync(path.join(ROOT, 'branding/ic_launcher_monochrome.svg'), monoSvg);

// ------------------------------------------------------------- bitmaps
let Resvg, sharp;
try { ({ Resvg } = require('@resvg/resvg-js')); sharp = require('sharp'); } catch (e) {
  console.log('vector layers written; install @resvg/resvg-js + sharp to also render PNG fallbacks');
  process.exit(0);
}
// Launchers show the inner 72dp of the 108dp canvas → viewBox 18..90.
const cropped = (clip) => `<svg xmlns="http://www.w3.org/2000/svg" width="72" height="72" viewBox="18 18 72 72">${defs}
  <defs><clipPath id="m">${clip}</clipPath></defs><g clip-path="url(#m)">${bgSvg}${fgSvg}</g></svg>`;
const shapes = {
  square: '<rect x="20" y="20" width="68" height="68" rx="16" ry="16"/>',
  round: '<circle cx="54" cy="54" r="35"/>',
  squircle: '<path d="M54,18 C80,18 90,28 90,54 C90,80 80,90 54,90 C28,90 18,80 18,54 C18,28 28,18 54,18 Z"/>',
  teardrop: '<path d="M54,18 A36,36 0 0 1 90,54 L90,82 a8,8 0 0 1 -8,8 L54,90 A36,36 0 0 1 54,18 Z"/>',
  full: '<rect x="18" y="18" width="72" height="72"/>',
};
const png = (svgText, size) => new Resvg(svgText, { fitTo: { mode: 'width', value: size } }).render().asPng();
const dens = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
(async () => {
  for (const [d, size] of Object.entries(dens)) {
    const dir = path.join(RES, `mipmap-${d}`);
    fs.mkdirSync(dir, { recursive: true });
    await sharp(png(cropped(shapes.square), size)).webp({ quality: 96 }).toFile(path.join(dir, 'ic_launcher.webp'));
    await sharp(png(cropped(shapes.round), size)).webp({ quality: 96 }).toFile(path.join(dir, 'ic_launcher_round.webp'));
  }
  fs.writeFileSync(path.join(ROOT, 'branding/ic_launcher-512.png'), png(cropped(shapes.full), 512));
  // preview sheet: every mask shape + small sizes + themed
  const tiles = [];
  let x = 16;
  for (const s of ['round', 'squircle', 'square', 'teardrop']) { tiles.push({ input: png(cropped(shapes[s]), 192), left: x, top: 16 }); x += 208; }
  for (const size of [96, 72, 48, 36, 24]) { tiles.push({ input: png(cropped(shapes.round), size), left: x, top: 16 }); x += size + 14; }
  const themed = (bg, fg) => `<svg xmlns="http://www.w3.org/2000/svg" width="72" height="72" viewBox="18 18 72 72"><circle cx="54" cy="54" r="36" fill="${bg}"/>${monoSvg.replace(/#000/g, fg).replace(/<\/?svg[^>]*>/g, '')}</svg>`;
  tiles.push({ input: png(themed('#D7E3FF', '#17305F'), 150), left: 16, top: 224 });
  tiles.push({ input: png(themed('#2A3550', '#C5D6FF'), 150), left: 182, top: 224 });
  tiles.push({ input: png(svg(bgSvg + fgSvg, '<circle cx="54" cy="54" r="33" fill="none"/>') , 324), left: 360, top: 224 });
  await sharp({ create: { width: 1180, height: 564, channels: 4, background: '#e9e9ee' } }).composite(tiles).png().toFile(path.join(ROOT, 'branding/preview.png'));
  console.log('icon layers, mipmaps and branding assets written');
})();
