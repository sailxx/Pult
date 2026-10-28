// Генератор логотипов Pult: три варианта, контуры букв берутся из шрифтов (fontkit), SVG без зависимостей от шрифтов.
import * as fontkit from 'fontkit';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const outRoot = path.resolve(here, '../../assets/logo');
const golos = fontkit.openSync(path.join(here, '.font/golos.ttf'));
const jb = fontkit.openSync(path.join(here, '.font/jbmono.ttf'));

const DARK = { bg: '#14171C', ink: '#E9E6DF', accent: '#E8B04A' };
const LIGHT = { bg: '#E9E6DF', ink: '#14171C', accent: '#B87A10' };

const n = (v) => +v.toFixed(2);

/** Раскладка строки: контуры, ширина, высота выносного элемента «l». */
function setText(base, wght, text, size, track) {
  const font = base.getVariation({ wght });
  const upm = font.unitsPerEm, s = size / upm;
  const r = font.layout(text);
  let x = 0;
  const glyphs = r.glyphs.map((g, i) => {
    const item = { g, x };
    x += r.positions[i].xAdvance + track * upm;
    return item;
  });
  x -= track * upm;
  // Точные границы по чернилам: от левого края первой буквы до правого края последней.
  const first = glyphs[0], last = glyphs[glyphs.length - 1];
  const left = first.g.bbox.minX;
  const right = last.x + last.g.bbox.maxX;
  const asc = font.layout('l').glyphs[0].bbox.maxY * s;
  return { glyphs, s, left, width: (right - left) * s, asc, font };
}

function textD(t, ox, baseline) {
  let d = '';
  const P = (px, py, gx) => `${n(ox + (px + gx - t.left) * t.s)} ${n(baseline - py * t.s)}`;
  for (const { g, x } of t.glyphs) {
    for (const c of g.path.commands) {
      const a = c.args;
      if (c.command === 'moveTo') d += `M${P(a[0], a[1], x)}`;
      else if (c.command === 'lineTo') d += `L${P(a[0], a[1], x)}`;
      else if (c.command === 'quadraticCurveTo') d += `Q${P(a[0], a[1], x)} ${P(a[2], a[3], x)}`;
      else if (c.command === 'bezierCurveTo') d += `C${P(a[0], a[1], x)} ${P(a[2], a[3], x)} ${P(a[4], a[5], x)}`;
      else if (c.command === 'closePath') d += 'Z';
    }
  }
  return d;
}

/* Знаки. Каждый рисуется в своих координатах 0..w x 0..h; mono = один цвет. */
const marks = {
  a: {
    w: 56, h: 58,
    draw: (c, mono) => `<g fill="${c.ink}">
<rect x="0" y="6" width="22" height="22"/><rect x="0" y="36" width="22" height="22"/><rect x="30" y="36" width="22" height="22"/>
<rect x="34" y="0" width="22" height="22" fill="${mono ? c.ink : c.accent}"/></g>`,
  },
  b: {
    w: 48, h: 56,
    draw: (c, mono) => `<path fill="${c.ink}" fill-rule="evenodd" d="M0 0H40L48 8V28L40 36H12V56H0Z M12 12V24H36V12Z"/>${mono ? '' : `<rect x="12" y="12" width="24" height="12" fill="${c.accent}"/>`}`,
  },
  c: {
    w: 44, h: 60,
    draw: (c, mono) => `<path d="M2.5 30V57.5H41.5V30" fill="none" stroke="${c.ink}" stroke-width="5" stroke-linejoin="miter"/>
<rect x="12.5" y="2.5" width="19" height="45" fill="none" stroke="${c.ink}" stroke-width="5"/>
<rect x="17" y="35" width="10" height="7" fill="${mono ? c.ink : c.accent}"/>`,
  },
};

const variants = {
  a: { slug: 'a-tile', name: 'A · Плитка', mark: marks.a, font: golos, wght: 600, text: 'pult', size: 56, track: -0.01, gap: 22 },
  b: { slug: 'b-slot', name: 'B · P со слотом', mark: marks.b, font: jb, wght: 500, text: 'pult', size: 50, track: 0, gap: 20 },
  c: { slug: 'c-dock', name: 'C · Док', mark: marks.c, font: golos, wght: 500, text: 'PULT', size: 38, track: 0.24, gap: 22 },
};

const svg = (w, h, body, bg) =>
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${n(w)} ${n(h)}" width="${n(w)}" height="${n(h)}">${bg ? `<rect width="100%" height="100%" fill="${bg}"/>` : ''}${body}</svg>\n`;

function markOnly(v, c, mono = false, pad = 0, bg) {
  return svg(v.mark.w + pad * 2, v.mark.h + pad * 2,
    `<g transform="translate(${pad} ${pad})">${v.mark.draw(c, mono)}</g>`, bg);
}

function lockup(v, c, mono = false, bg) {
  const t = setText(v.font, v.wght, v.text, v.size, v.track);
  const pad = 16;
  const mw = v.mark.w, mh = v.mark.h;
  const ox = pad + mw + v.gap;
  const baseline = pad + mh / 2 + t.asc / 2;
  const w = ox + t.width + pad;
  const body = `<g transform="translate(${pad} ${pad})">${v.mark.draw(c, mono)}</g>` +
    `<path fill="${c.ink}" d="${textD(t, ox, baseline)}"/>`;
  return svg(w, mh + pad * 2, body, bg);
}

function stacked(v, c, mono = false, bg) {
  const t = setText(v.font, v.wght, v.text, v.size * 0.8, v.track);
  const pad = 20, gap = 20;
  const w = Math.max(v.mark.w, t.width) + pad * 2;
  const mx = (w - v.mark.w) / 2;
  const baseline = pad + v.mark.h + gap + t.asc;
  const h = baseline + t.asc * 0.35 + pad; // запас под нижний выносной элемент «p»
  const body = `<g transform="translate(${n(mx)} ${pad})">${v.mark.draw(c, mono)}</g>` +
    `<path fill="${c.ink}" d="${textD(t, (w - t.width) / 2, baseline)}"/>`;
  return svg(w, h, body, bg);
}

/** Иконка приложения: знак на тёмной плите (маска Android сама скругляет углы). */
function icon(v, c) {
  const S = 108, k = (S * 0.5) / Math.max(v.mark.w, v.mark.h);
  const w = v.mark.w * k, h = v.mark.h * k;
  return svg(S, S, `<g transform="translate(${n((S - w) / 2)} ${n((S - h) / 2)}) scale(${n(k)})">${v.mark.draw(c, false)}</g>`, c.bg);
}

const results = {};
for (const [key, v] of Object.entries(variants)) {
  const dir = path.join(outRoot, v.slug);
  fs.mkdirSync(dir, { recursive: true });
  const files = {
    'mark-dark.svg': markOnly(v, DARK),
    'mark-light.svg': markOnly(v, LIGHT),
    'mark-mono.svg': markOnly(v, { ink: 'currentColor', accent: 'currentColor' }, true),
    'lockup-dark.svg': lockup(v, DARK),
    'lockup-light.svg': lockup(v, LIGHT),
    'lockup-mono.svg': lockup(v, { ink: 'currentColor', accent: 'currentColor' }, true),
    'stacked-dark.svg': stacked(v, DARK),
    'stacked-light.svg': stacked(v, LIGHT),
    'icon.svg': icon(v, DARK),
  };
  for (const [f, s] of Object.entries(files)) fs.writeFileSync(path.join(dir, f), s);
  results[key] = { v, files };
}

/* Лист для выбора. Шрифт встроен в base64, чтобы файл открывался где угодно. */
const b64 = (f) => fs.readFileSync(path.join(here, '.font', f)).toString('base64');
const inline = (s) => s.replace(/<svg[^>]*?width="[\d.]+" height="[\d.]+">/, (m) => m.replace(/ width="[\d.]+" height="[\d.]+"/, ''));

const notes = {
  a: 'Четыре плитки: три на месте, одна приподнята и горит янтарём. Это сетка док-панели и жест «выбрал, запустил». Строчное «pult» в Golos Text SemiBold, плотный кернинг.',
  b: 'Буква P из прямых блоков со срезанными углами; в её просвете горит янтарный слот, как экран или кнопка. Моноширинное «pult» в JetBrains Mono: тот же шрифт, что в моих README.',
  c: 'Телефон стоит в подставке: «пульт и док» в одном знаке. Контурная линия одной толщины, янтарная точка касания. Капитель PULT с широким трекингом в Golos Text Medium.',
};

const sizes = [16, 24, 32, 48, 64];
const section = (key) => {
  const { v, files } = results[key];
  const sz = sizes.map((s) => `<div class="sz"><div style="width:${s}px;height:${s}px">${inline(files['mark-dark.svg'])}</div><small>${s}</small></div>`).join('');
  const szl = sizes.slice(0, 3).map((s) => `<div class="sz"><div style="width:${s}px;height:${s}px;color:#14171C">${inline(files['mark-mono.svg'])}</div><small>${s}</small></div>`).join('');
  return `<section>
<h2>${v.name}</h2><p class="note">${notes[key]}</p>
<div class="grid">
<div class="panel dark wide"><div class="lk">${inline(files['lockup-dark.svg'])}</div></div>
<div class="panel light wide"><div class="lk">${inline(files['lockup-light.svg'])}</div></div>
<div class="panel dark"><div class="st">${inline(files['stacked-dark.svg'])}</div><small>Вертикальный вариант</small></div>
<div class="panel icon"><div class="ic">${inline(files['icon.svg'])}</div><small>Иконка приложения</small></div>
<div class="panel dark"><div class="row">${sz}</div><small>Знак в малых размерах, px</small></div>
<div class="panel light"><div class="row">${szl}</div><small>Одноцветный знак на светлом</small></div>
</div></section>`;
};

const html = `<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Pult, логотипы</title><style>
@font-face{font-family:G;src:url(data:font/ttf;base64,${b64('golos.ttf')});font-weight:400 900}
:root{--bg:#101216;--ink:#E9E6DF;--dim:#8B929C;--line:#2B313A}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font-family:G,system-ui,sans-serif;padding:32px 16px 64px}
main{max-width:1040px;margin:0 auto}h1{font-size:28px;margin:0 0 6px;font-weight:600}.lead{color:var(--dim);margin:0 0 40px;font-size:15px}
section{margin:0 0 56px}h2{font-size:20px;margin:0 0 6px;font-weight:600}.note{color:var(--dim);max-width:70ch;margin:0 0 18px;font-size:14px;line-height:1.55}
.grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.panel{border:1px solid var(--line);padding:24px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;min-height:150px}
.panel.dark{background:#14171C}.panel.light{background:#E9E6DF;color:#14171C;border-color:#E9E6DF}.panel.wide{min-height:190px}
.panel small{color:var(--dim);font-size:12px}.panel.light small{color:#5b616a}
.lk svg{height:84px;width:auto;max-width:100%}.st svg{height:150px;width:auto}.ic svg{width:96px;height:96px;outline:1px solid #3a414c}.panel.icon{background:#0B0D10}
.row{display:flex;align-items:flex-end;gap:22px;flex-wrap:wrap;justify-content:center}.sz{display:flex;flex-direction:column;align-items:center;gap:6px}.sz svg{width:100%;height:100%;display:block}
.grid>*{min-width:0}.row{gap:14px}
@media(max-width:640px){.grid{grid-template-columns:minmax(0,1fr)}.lk svg{height:56px}.panel{padding:18px 12px}}
</style></head><body><main>
<h1>Pult: три варианта логотипа</h1>
<p class="lead">Палитра: графит #14171C, тёплый светлый #E9E6DF, янтарь #E8B04A (на светлом #B87A10). Все буквы переведены в контуры.</p>
${['a', 'b', 'c'].map(section).join('')}
</main></body></html>`;
fs.writeFileSync(path.join(here, 'preview.html'), html);
console.log('ok', Object.keys(results).join(','));
