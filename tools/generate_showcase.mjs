import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const outDir = path.resolve(here, '../assets/screenshots');
const iconsDir = path.resolve(here, '../assets/app-icons');

function getBase64Icon(fileName) {
  const p = path.join(iconsDir, fileName);
  if (fs.existsSync(p)) {
    return 'data:image/png;base64,' + fs.readFileSync(p).toString('base64');
  }
  return '';
}

const vscodeIco = getBase64Icon('vscode.png');
const braveIco = getBase64Icon('brave.png');
const chromeIco = getBase64Icon('chrome.png');
const telegramIco = getBase64Icon('telegram.png');
const vlcIco = getBase64Icon('vlc.png');
const explorerIco = getBase64Icon('explorer.png');
const mpvIco = getBase64Icon('mpv.png');

const STYLES = `
  text { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
  .mono { font-family: 'Consolas', 'Courier New', monospace; }
  .liquid-glass { fill: #FFFFFF; fill-opacity: 0.08; stroke: #FFFFFF; stroke-opacity: 0.12; stroke-width: 1; }
  .liquid-card { fill: #FFFFFF; fill-opacity: 0.06; stroke: #FFFFFF; stroke-opacity: 0.10; stroke-width: 1; }
  .liquid-active { fill: #FFFFFF; fill-opacity: 0.16; stroke: #FFFFFF; stroke-opacity: 0.20; stroke-width: 1; }
  .liquid-btn { fill: #FFFFFF; fill-opacity: 0.08; stroke: #FFFFFF; stroke-opacity: 0.12; stroke-width: 1; }
  .accent-fill { fill: #38BDF8; }
`;

function darkMatteBackground(w, h) {
  return `
    <defs>
      <radialGradient id="darkVignette" cx="50%" cy="50%" r="70%">
        <stop offset="0%" stop-color="#141820" stop-opacity="0.8"/>
        <stop offset="100%" stop-color="#0A0C10" stop-opacity="1"/>
      </radialGradient>
    </defs>
    <rect width="${w}" height="${h}" fill="#0C0E12"/>
    <rect width="${w}" height="${h}" fill="url(#darkVignette)"/>
  `;
}

function headerBar(activeTab = 'Dock') {
  const tabs = ['Dock', 'Touchpad', 'Clipboard', 'Signature', 'Capture', 'Apps'];
  let tabX = 3;
  let tabsSvg = '';

  for (const t of tabs) {
    const isActive = t === activeTab;
    const tw = t.length > 7 ? 96 : 82;
    if (isActive) {
      tabsSvg += `
        <rect x="${tabX}" y="3" width="${tw}" height="28" rx="14" fill="#FFFFFF" fill-opacity="0.16" stroke="#FFFFFF" stroke-opacity="0.22"/>
        <text x="${tabX + tw/2}" y="21" fill="#FFFFFF" font-size="11" font-weight="700" text-anchor="middle">${t}</text>
      `;
    } else {
      tabsSvg += `
        <text x="${tabX + tw/2}" y="21" fill="#9CA3AF" font-size="11" font-weight="500" text-anchor="middle">${t}</text>
      `;
    }
    tabX += tw + 3;
  }

  return `
    <!-- Floating Liquid Glass Header Bar -->
    <g transform="translate(28 16)">
      <!-- Left: Host status pill -->
      <rect x="0" y="0" width="186" height="34" rx="17" class="liquid-glass"/>
      <circle cx="16" cy="17" r="3.5" fill="#38BDF8"/>
      <text x="28" y="21" fill="#F3F4F6" font-size="11.5" font-weight="600">DESKTOP-6T5OKAJ</text>
      <text x="154" y="21" fill="#6B7280" font-size="10" class="mono">LAN</text>

      <!-- Center: Segmented Navigation capsule -->
      <g transform="translate(300 0)">
        <rect x="0" y="0" width="${tabX + 3}" height="34" rx="17" class="liquid-glass"/>
        ${tabsSvg}
      </g>

      <!-- Right: Settings and Disconnect -->
      <g transform="translate(1148 0)">
        <rect x="0" y="0" width="34" height="34" rx="17" class="liquid-glass"/>
        <text x="17" y="22" fill="#9CA3AF" font-size="13" text-anchor="middle">⚙</text>

        <rect x="42" y="0" width="34" height="34" rx="17" class="liquid-glass"/>
        <text x="59" y="21" fill="#9CA3AF" font-size="11" font-weight="600" text-anchor="middle">✕</text>
      </g>
    </g>
  `;
}

// 1. DOCK & APPS SCREEN
function buildScreenDock() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Liquid Glass Landscape Dock">
<style>${STYLES}</style>
${darkMatteBackground(1280, 720)}
${headerBar('Dock')}

<!-- Dock Sub-header -->
<g transform="translate(36 74)">
  <text x="0" y="16" fill="#6B7280" font-size="11" font-weight="700" letter-spacing="0.8">MAIN DOCK</text>
  <rect x="94" y="2" width="86" height="20" rx="10" fill="#FFFFFF" fill-opacity="0.08" stroke="#FFFFFF" stroke-opacity="0.12"/>
  <text x="137" y="15" fill="#38BDF8" font-size="10" font-weight="600" text-anchor="middle">6 Pinned</text>

  <g transform="translate(1030 0)">
    <rect x="0" y="0" width="178" height="26" rx="13" class="liquid-glass"/>
    <text x="89" y="17" fill="#38BDF8" font-size="11" font-weight="500" text-anchor="middle">All Applications (42) →</text>
  </g>
</g>

<!-- 6 Pure Floating App Logos (Zero square card boxes, Zero borders, Larger official icons) -->
<g transform="translate(100 170)">
  <!-- App 1: Brave Browser -->
  <g transform="translate(0 0)">
    <image href="${braveIco}" x="18" y="0" width="108" height="108"/>
    <circle cx="72" cy="120" r="4" fill="#38BDF8"/>
    <text x="72" y="148" fill="#F3F4F6" font-size="13" font-weight="600" text-anchor="middle">Brave</text>
  </g>

  <!-- App 2: Google Chrome -->
  <g transform="translate(188 0)">
    <image href="${chromeIco}" x="18" y="0" width="108" height="108"/>
    <circle cx="72" cy="120" r="4" fill="#38BDF8"/>
    <text x="72" y="148" fill="#F3F4F6" font-size="13" font-weight="600" text-anchor="middle">Chrome</text>
  </g>

  <!-- App 3: Visual Studio Code -->
  <g transform="translate(376 0)">
    <image href="${vscodeIco}" x="18" y="0" width="108" height="108"/>
    <circle cx="72" cy="120" r="4" fill="#38BDF8"/>
    <text x="72" y="148" fill="#F3F4F6" font-size="13" font-weight="600" text-anchor="middle">VS Code</text>
  </g>

  <!-- App 4: Telegram Desktop -->
  <g transform="translate(564 0)">
    <image href="${telegramIco}" x="18" y="0" width="108" height="108"/>
    <circle cx="72" cy="120" r="4" fill="#38BDF8"/>
    <text x="72" y="148" fill="#F3F4F6" font-size="13" font-weight="600" text-anchor="middle">Telegram</text>
  </g>

  <!-- App 5: VLC Media Player -->
  <g transform="translate(752 0)">
    <image href="${vlcIco}" x="18" y="0" width="108" height="108"/>
    <text x="72" y="148" fill="#9CA3AF" font-size="13" font-weight="500" text-anchor="middle">VLC Player</text>
  </g>

  <!-- App 6: Windows Explorer -->
  <g transform="translate(940 0)">
    <image href="${explorerIco}" x="18" y="0" width="108" height="108"/>
    <circle cx="72" cy="120" r="4" fill="#38BDF8"/>
    <text x="72" y="148" fill="#F3F4F6" font-size="13" font-weight="600" text-anchor="middle">Explorer</text>
  </g>
</g>

<!-- FLOATING LIQUID GLASS NOW PLAYING AND VOLUME PILL -->
<g transform="translate(320 626)">
  <rect x="0" y="0" width="640" height="52" rx="26" class="liquid-glass"/>

  <!-- Left: Volume controls -->
  <g transform="translate(20 12)">
    <rect x="0" y="0" width="64" height="28" rx="14" fill="#FFFFFF" fill-opacity="0.08" stroke="#FFFFFF" stroke-opacity="0.14"/>
    <text x="32" y="18" fill="#38BDF8" font-size="11.5" font-weight="700" text-anchor="middle">70%</text>

    <circle cx="82" cy="14" r="14" class="liquid-btn"/>
    <text x="82" y="19" fill="#F3F4F6" font-size="14" font-weight="700" text-anchor="middle">−</text>

    <circle cx="118" cy="14" r="14" class="liquid-btn"/>
    <text x="118" y="19" fill="#F3F4F6" font-size="14" font-weight="700" text-anchor="middle">+</text>
  </g>

  <!-- Center: Media controls -->
  <g transform="translate(268 12)">
    <circle cx="14" cy="14" r="14" class="liquid-btn"/>
    <path d="M18 9 L11 14 L18 19 Z M10 9 L10 19" stroke="#F3F4F6" stroke-width="1.8" fill="none" stroke-linecap="round"/>

    <circle cx="52" cy="14" r="16" fill="#38BDF8"/>
    <path d="M49 8 L58 14 L49 20 Z" fill="#0C0E12"/>

    <circle cx="90" cy="14" r="14" class="liquid-btn"/>
    <path d="M86 9 L93 14 L86 19 Z M94 9 L94 19" stroke="#F3F4F6" stroke-width="1.8" fill="none" stroke-linecap="round"/>
  </g>

  <!-- Right: System Actions -->
  <g transform="translate(440 12)">
    <rect x="0" y="0" width="88" height="28" rx="14" class="liquid-btn"/>
    <text x="44" y="18" fill="#9CA3AF" font-size="10.5" font-weight="500" text-anchor="middle">Desktop</text>

    <rect x="96" y="0" width="76" height="28" rx="14" class="liquid-btn"/>
    <text x="134" y="18" fill="#EF4444" font-size="10.5" font-weight="500" text-anchor="middle">Lock PC</text>
  </g>
</g>
</svg>`;
}

// 2. TOUCHPAD SCREEN
function buildScreenTouchpad() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Touchpad">
<style>${STYLES}</style>
${darkMatteBackground(1280, 720)}
${headerBar('Touchpad')}

<g transform="translate(36 74)">
  <!-- Left Trackpad Area (880px) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="880" height="610" rx="20" class="liquid-card"/>

    <!-- Subtle center crosshair guide -->
    <circle cx="440" cy="275" r="32" stroke="#FFFFFF" stroke-opacity="0.08" stroke-width="1" fill="none"/>
    <circle cx="440" cy="275" r="4" fill="#38BDF8" fill-opacity="0.5"/>

    <text x="440" y="335" fill="#4B5563" font-size="12" font-weight="500" text-anchor="middle">Precision touch surface with kinetic motion</text>

    <!-- Bottom Left/Right Mouse Click Buttons in Liquid Glass -->
    <g transform="translate(20 540)">
      <rect x="0" y="0" width="410" height="52" rx="16" class="liquid-glass"/>
      <text x="205" y="31" fill="#F3F4F6" font-size="12.5" font-weight="600" text-anchor="middle">Left Click (LMB)</text>

      <rect x="430" y="0" width="410" height="52" rx="16" class="liquid-glass"/>
      <text x="635" y="31" fill="#9CA3AF" font-size="12.5" font-weight="600" text-anchor="middle">Right Click (RMB)</text>
    </g>
  </g>

  <!-- Right Quick Actions Column (310px) -->
  <g transform="translate(900 0)">
    <rect x="0" y="0" width="308" height="610" rx="20" class="liquid-card"/>

    <g transform="translate(24 24)">
      <text x="0" y="16" fill="#6B7280" font-size="11" font-weight="700" letter-spacing="0.6">GESTURES &amp; SENSITIVITY</text>

      <!-- Sensitivity Control -->
      <g transform="translate(0 40)">
        <rect x="0" y="0" width="260" height="64" rx="14" class="liquid-glass"/>
        <text x="16" y="26" fill="#F3F4F6" font-size="12" font-weight="600">Pointer Speed</text>
        <text x="244" y="26" fill="#38BDF8" font-size="12" font-weight="700" text-anchor="end">1.4×</text>
        <rect x="16" y="42" width="228" height="5" rx="2.5" fill="#FFFFFF" fill-opacity="0.12"/>
        <rect x="16" y="42" width="130" height="5" rx="2.5" fill="#38BDF8"/>
        <circle cx="146" cy="44.5" r="7" fill="#FFFFFF"/>
      </g>

      <!-- Gesture Shortcuts -->
      <g transform="translate(0 120)">
        <text x="0" y="14" fill="#6B7280" font-size="10.5" font-weight="700">MULTITOUCH GESTURES</text>

        <rect x="0" y="26" width="260" height="42" rx="12" class="liquid-glass"/>
        <text x="16" y="52" fill="#F3F4F6" font-size="12">1 finger: move cursor</text>

        <rect x="0" y="76" width="260" height="42" rx="12" class="liquid-glass"/>
        <text x="16" y="102" fill="#F3F4F6" font-size="12">2 fingers: vertical scroll</text>

        <rect x="0" y="126" width="260" height="42" rx="12" class="liquid-glass"/>
        <text x="16" y="152" fill="#F3F4F6" font-size="12">2-finger tap: context menu</text>

        <rect x="0" y="176" width="260" height="42" rx="12" class="liquid-glass"/>
        <text x="16" y="202" fill="#F3F4F6" font-size="12">Double tap: Drag &amp; Drop</text>
      </g>
    </g>
  </g>
</g>
</svg>`;
}

// 3. CLIPBOARD SCREEN
function buildScreenClipboard() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Clipboard">
<style>${STYLES}</style>
${darkMatteBackground(1280, 720)}
${headerBar('Clipboard')}

<g transform="translate(36 74)">
  <!-- Left Column: Windows Clipboard (760px) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="760" height="610" rx="20" class="liquid-card"/>

    <g transform="translate(24 24)">
      <circle cx="6" cy="8" r="3.5" fill="#38BDF8"/>
      <text x="18" y="12" fill="#6B7280" font-size="11" font-weight="700" letter-spacing="0.6">CURRENT WINDOWS CLIPBOARD</text>

      <!-- Active Clip Preview -->
      <g transform="translate(0 32)">
        <rect x="0" y="0" width="712" height="150" rx="16" class="liquid-glass"/>
        <text x="24" y="38" fill="#38BDF8" font-size="11" font-weight="600">Synced • Just now</text>
        <text x="24" y="74" fill="#F3F4F6" font-size="13.5" class="mono">cd pult &amp;&amp; host\\build.cmd</text>
        <text x="24" y="112" fill="#6B7280" font-size="12" class="mono">// Pult Windows Host: Peer-to-peer keep-alive latency 1.04ms</text>
      </g>

      <!-- History Clips -->
      <g transform="translate(0 200)">
        <text x="0" y="16" fill="#6B7280" font-size="10.5" font-weight="700">CLIPBOARD HISTORY</text>

        <rect x="0" y="30" width="712" height="52" rx="14" class="liquid-glass"/>
        <text x="20" y="61" fill="#E5E7EB" font-size="12.5" class="mono">https://github.com/sailxx/Pult</text>
        <text x="692" y="61" fill="#6B7280" font-size="11" text-anchor="end">12:02</text>

        <rect x="0" y="90" width="712" height="52" rx="14" class="liquid-glass"/>
        <text x="20" y="121" fill="#E5E7EB" font-size="12.5" class="mono">git commit -m "feat(ui): Liquid Glass Apple standard"</text>
        <text x="692" y="121" fill="#6B7280" font-size="11" text-anchor="end">11:58</text>

        <rect x="0" y="150" width="712" height="52" rx="14" class="liquid-glass"/>
        <text x="20" y="181" fill="#E5E7EB" font-size="12.5" class="mono">DESKTOP-6T5OKAJ:4848</text>
        <text x="692" y="181" fill="#6B7280" font-size="11" text-anchor="end">11:45</text>
      </g>
    </g>
  </g>

  <!-- Right Column: Quick Send (430px) -->
  <g transform="translate(780 0)">
    <rect x="0" y="0" width="428" height="610" rx="20" class="liquid-card"/>

    <g transform="translate(24 24)">
      <text x="0" y="16" fill="#6B7280" font-size="11" font-weight="700" letter-spacing="0.6">PUSH TEXT TO PC</text>

      <g transform="translate(0 32)">
        <rect x="0" y="0" width="380" height="240" rx="16" class="liquid-glass"/>
        <text x="20" y="36" fill="#6B7280" font-size="12">Type or paste text or links to inject into PC clipboard...</text>
      </g>

      <g transform="translate(0 292)">
        <rect x="0" y="0" width="380" height="50" rx="25" fill="#38BDF8"/>
        <text x="190" y="30" fill="#0C0E12" font-size="13" font-weight="700" text-anchor="middle">Inject into Windows Clipboard</text>
      </g>
    </g>
  </g>
</g>
</svg>`;
}

// 4. SIGNATURE CANVAS
function buildScreenSignature() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Digital Signature">
<style>${STYLES}</style>
${darkMatteBackground(1280, 720)}
${headerBar('Signature')}

<g transform="translate(36 74)">
  <!-- Left: Signature Canvas Area (900px) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="900" height="610" rx="20" class="liquid-card"/>

    <g transform="translate(24 24)">
      <circle cx="6" cy="6" r="3.5" fill="#38BDF8"/>
      <text x="18" y="10" fill="#6B7280" font-size="11" font-weight="700" letter-spacing="0.6">DIGITAL SIGNATURE FOR DOCUMENTS</text>
      <text x="850" y="10" fill="#6B7280" font-size="11" text-anchor="end">Bézier smoothing</text>
    </g>

    <!-- Drawn vector curve signature -->
    <path d="M 120 380 Q 200 160 310 320 T 480 270 Q 580 180 660 360 T 820 310" stroke="#FFFFFF" stroke-width="3.5" fill="none" stroke-linecap="round" stroke-linejoin="round"/>

    <!-- Bottom baseline -->
    <line x1="80" y1="460" x2="820" y2="460" stroke="#FFFFFF" stroke-opacity="0.12" stroke-width="1.5" stroke-dasharray="8 8"/>
    <text x="450" y="490" fill="#4B5563" font-size="11.5" text-anchor="middle">Signature baseline • Sign with finger or active stylus</text>
  </g>

  <!-- Right: Actions (290px) -->
  <g transform="translate(920 0)">
    <rect x="0" y="0" width="288" height="610" rx="20" class="liquid-card"/>

    <g transform="translate(24 24)">
      <text x="0" y="16" fill="#6B7280" font-size="11" font-weight="700">ACTIONS</text>

      <g transform="translate(0 40)">
        <rect x="0" y="0" width="240" height="50" rx="25" fill="#38BDF8"/>
        <text x="120" y="30" fill="#0C0E12" font-size="12.5" font-weight="700" text-anchor="middle">Insert into PC Document</text>

        <rect x="0" y="64" width="240" height="46" rx="23" class="liquid-btn"/>
        <text x="120" y="93" fill="#F3F4F6" font-size="12" font-weight="600" text-anchor="middle">Clear Canvas</text>

        <rect x="0" y="122" width="240" height="46" rx="23" class="liquid-btn"/>
        <text x="120" y="151" fill="#F3F4F6" font-size="12" font-weight="600" text-anchor="middle">Save as PNG</text>
      </g>
    </g>
  </g>
</g>
</svg>`;
}

// 5. SCREEN CAPTURE
function buildScreenCapture() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Screen Capture">
<style>${STYLES}</style>
${darkMatteBackground(1280, 720)}
${headerBar('Capture')}

<g transform="translate(36 74)">
  <!-- Left: Screenshot Preview (910px) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="910" height="610" rx="20" class="liquid-card"/>

    <g transform="translate(24 20)">
      <circle cx="6" cy="6" r="3.5" fill="#38BDF8"/>
      <text x="18" y="10" fill="#6B7280" font-size="11" font-weight="700" letter-spacing="0.6">WINDOWS SCREEN DISPLAY CAPTURE</text>
      <text x="860" y="10" fill="#6B7280" font-size="11" text-anchor="end">Time: 12:00:24</text>
    </g>

    <!-- Desktop Preview Frame -->
    <g transform="translate(20 48)">
      <rect x="0" y="0" width="870" height="540" rx="14" fill="#0C0E14" stroke="#FFFFFF" stroke-opacity="0.10"/>

      <!-- Window 1: VS Code Editor -->
      <g transform="translate(80 50)">
        <rect x="0" y="0" width="520" height="340" rx="10" fill="#181A20" stroke="#FFFFFF" stroke-opacity="0.10"/>
        <rect x="0" y="0" width="520" height="32" rx="10" fill="#20232B"/>
        <circle cx="20" cy="16" r="4.5" fill="#EF4444"/>
        <circle cx="36" cy="16" r="4.5" fill="#F59E0B"/>
        <circle cx="52" cy="16" r="4.5" fill="#22C55E"/>
        <text x="260" y="20" fill="#9CA3AF" font-size="11" text-anchor="middle">Pult.cs — Visual Studio Code</text>

        <!-- Code lines -->
        <text x="24" y="66" fill="#569CD6" font-size="12" class="mono">namespace <tspan fill="#4EC9B0">Pult</tspan> {</text>
        <text x="44" y="92" fill="#569CD6" font-size="12" class="mono">public class <tspan fill="#4EC9B0">Server</tspan> {</text>
        <text x="64" y="118" fill="#DCDCAA" font-size="12" class="mono">StartServer<tspan fill="#D4D4D4">();</tspan></text>
        <text x="64" y="144" fill="#6A9955" font-size="12" class="mono">// Pult Liquid Glass Redesign: Verified</text>
        <text x="44" y="170" fill="#D4D4D4" font-size="12" class="mono">}</text>
        <text x="24" y="196" fill="#D4D4D4" font-size="12" class="mono">}</text>
      </g>

      <!-- Taskbar -->
      <g transform="translate(0 496)">
        <rect x="0" y="0" width="870" height="44" fill="#12151C"/>
        <rect x="415" y="13" width="18" height="18" fill="#38BDF8" rx="3"/>
        <circle cx="455" cy="22" r="9" fill="#2563EB"/>
        <circle cx="485" cy="22" r="9" fill="#10B981"/>
      </g>
    </g>
  </g>

  <!-- Right: Actions (280px) -->
  <g transform="translate(930 0)">
    <rect x="0" y="0" width="278" height="610" rx="20" class="liquid-card"/>

    <g transform="translate(24 24)">
      <text x="0" y="16" fill="#6B7280" font-size="11" font-weight="700">ACTIONS</text>

      <g transform="translate(0 40)">
        <rect x="0" y="0" width="230" height="50" rx="25" fill="#38BDF8"/>
        <text x="115" y="30" fill="#0C0E12" font-size="12.5" font-weight="700" text-anchor="middle">Capture Screen</text>

        <rect x="0" y="64" width="230" height="46" rx="23" class="liquid-btn"/>
        <text x="115" y="93" fill="#F3F4F6" font-size="12" font-weight="600" text-anchor="middle">To PC Clipboard</text>

        <rect x="0" y="122" width="230" height="46" rx="23" class="liquid-btn"/>
        <text x="115" y="151" fill="#F3F4F6" font-size="12" font-weight="600" text-anchor="middle">Save to PC Disk</text>
      </g>
    </g>
  </g>
</g>
</svg>`;
}

// 6. WINDOWS 11 COMPANION TRAY FLYOUT (SAFE VALID XML - NO BARE AMPERSANDS)
function buildWindowsTrayFlyout() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="320" height="330" viewBox="0 0 320 330" role="img" aria-label="Windows 11 Volume and Pult Companion Flyout">
<style>
  text { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
</style>
<defs>
  <clipPath id="flyoutRound">
    <rect width="320" height="330" rx="12"/>
  </clipPath>
</defs>

<g clip-path="url(#flyoutRound)">
  <!-- Fluent Dark Surface -->
  <rect width="320" height="330" fill="#1C1C1C"/>

  <!-- Row 1: Speakers (Realtek(R) Audio) -->
  <g transform="translate(0 0)">
    <!-- Active Vertical Cyan Indicator -->
    <rect x="4" y="12" width="3" height="20" rx="1.5" fill="#38A7FF"/>

    <!-- Speaker Icon -->
    <g transform="translate(14 12)">
      <path d="M3 6 L7 6 L11 2 L11 16 L7 12 L3 12 Z" fill="#E5E7EB"/>
      <path d="M13 5 Q16 9 13 13 M15 3 Q19 9 15 15" stroke="#E5E7EB" stroke-width="1.4" fill="none" stroke-linecap="round"/>
    </g>

    <!-- Title -->
    <text x="38" y="21" fill="#F3F4F6" font-size="12" font-weight="500">Speakers (Realtek(R) Audio)</text>

    <!-- Value -->
    <text x="306" y="37" fill="#F3F4F6" font-size="13" font-weight="500" text-anchor="end">70</text>

    <!-- Slider -->
    <rect x="38" y="32" width="250" height="4" rx="2" fill="#555555"/>
    <rect x="38" y="32" width="175" height="4" rx="2" fill="#38A7FF"/>
    <circle cx="213" cy="34" r="8" fill="#38A7FF" stroke="#1C1C1C" stroke-width="2"/>

    <!-- Divider -->
    <line x1="12" y1="56" x2="308" y2="56" stroke="#2C2C2C" stroke-width="1"/>
  </g>

  <!-- Row 2: Brave Browser -->
  <g transform="translate(0 58)">
    <!-- Brave Lion Icon -->
    <image href="${braveIco}" x="14" y="10" width="18" height="18"/>

    <!-- Title -->
    <text x="38" y="21" fill="#F3F4F6" font-size="12" font-weight="500">Brave Browser</text>

    <!-- Value -->
    <text x="306" y="37" fill="#F3F4F6" font-size="13" font-weight="500" text-anchor="end">100</text>

    <!-- Slider (Full 100) -->
    <rect x="38" y="32" width="250" height="4" rx="2" fill="#38A7FF"/>
    <circle cx="288" cy="34" r="8" fill="#38A7FF" stroke="#1C1C1C" stroke-width="2"/>

    <!-- Divider -->
    <line x1="12" y1="56" x2="308" y2="56" stroke="#2C2C2C" stroke-width="1"/>
  </g>

  <!-- Row 3: MAG 271QP X28 (Monitor Brightness) -->
  <g transform="translate(0 116)">
    <!-- Sun Icon -->
    <g transform="translate(14 10)">
      <circle cx="9" cy="9" r="4" fill="none" stroke="#E5E7EB" stroke-width="1.4"/>
      <path d="M9 1 L9 3 M9 15 L9 17 M1 9 L3 9 M15 9 L17 9 M3.5 3.5 L5 5 M13 13 L14.5 14.5 M3.5 14.5 L5 13 M13 5 L14.5 3.5" stroke="#E5E7EB" stroke-width="1.3" stroke-linecap="round"/>
    </g>

    <!-- Title -->
    <text x="38" y="21" fill="#F3F4F6" font-size="12" font-weight="500">MAG 271QP X28</text>

    <!-- Value -->
    <text x="306" y="37" fill="#F3F4F6" font-size="13" font-weight="500" text-anchor="end">100</text>

    <!-- Slider (Full 100) -->
    <rect x="38" y="32" width="250" height="4" rx="2" fill="#38A7FF"/>
    <circle cx="288" cy="34" r="8" fill="#38A7FF" stroke="#1C1C1C" stroke-width="2"/>

    <!-- Divider -->
    <line x1="12" y1="56" x2="308" y2="56" stroke="#2C2C2C" stroke-width="1"/>
  </g>

  <!-- Row 4: Night Light -->
  <g transform="translate(0 174)">
    <!-- Crescent Moon Icon -->
    <g transform="translate(14 10)">
      <path d="M14 10.5 A6 6 0 1 1 7.5 4 A5 5 0 0 0 14 10.5 Z" fill="none" stroke="#E5E7EB" stroke-width="1.4" stroke-linejoin="round"/>
    </g>

    <!-- Title -->
    <text x="38" y="21" fill="#F3F4F6" font-size="12" font-weight="500">Night light</text>

    <!-- Value -->
    <text x="306" y="37" fill="#F3F4F6" font-size="13" font-weight="500" text-anchor="end">0</text>

    <!-- Slider (0 Value) -->
    <rect x="38" y="32" width="250" height="4" rx="2" fill="#555555"/>
    <circle cx="44" cy="34" r="8" fill="#38A7FF" stroke="#1C1C1C" stroke-width="2"/>

    <!-- Divider -->
    <line x1="12" y1="56" x2="308" y2="56" stroke="#2C2C2C" stroke-width="1"/>
  </g>

  <!-- Row 5: Dimming -->
  <g transform="translate(0 232)">
    <!-- Dimmer Sun Icon -->
    <g transform="translate(14 10)">
      <circle cx="9" cy="9" r="3.5" fill="none" stroke="#E5E7EB" stroke-width="1.3"/>
      <path d="M9 2 L9 3.5 M9 14.5 L9 16 M2 9 L3.5 9 M14.5 9 L16 9" stroke="#E5E7EB" stroke-width="1.3" stroke-linecap="round"/>
    </g>

    <!-- Title -->
    <text x="38" y="21" fill="#F3F4F6" font-size="12" font-weight="500">Dimming</text>

    <!-- Value -->
    <text x="306" y="37" fill="#F3F4F6" font-size="13" font-weight="500" text-anchor="end">0</text>

    <!-- Slider (0 Value) -->
    <rect x="38" y="32" width="250" height="4" rx="2" fill="#555555"/>
    <circle cx="44" cy="34" r="8" fill="#38A7FF" stroke="#1C1C1C" stroke-width="2"/>
  </g>

  <!-- Bottom Footer Bar -->
  <g transform="translate(0 286)">
    <rect width="320" height="44" fill="#161616"/>

    <!-- Left Label -->
    <text x="14" y="27" fill="#F3F4F6" font-size="12.5" font-weight="500">Volume</text>

    <!-- Right Icons: Mixer and Settings -->
    <!-- Equalizer / Mixer Glyph -->
    <g transform="translate(250 14)">
      <line x1="3" y1="4" x2="15" y2="4" stroke="#E5E7EB" stroke-width="1.4" stroke-linecap="round"/>
      <line x1="3" y1="12" x2="15" y2="12" stroke="#E5E7EB" stroke-width="1.4" stroke-linecap="round"/>
      <circle cx="6" cy="4" r="2.5" fill="#161616" stroke="#E5E7EB" stroke-width="1.4"/>
      <circle cx="12" cy="12" r="2.5" fill="#161616" stroke="#E5E7EB" stroke-width="1.4"/>
    </g>

    <!-- Settings Gear Glyph -->
    <g transform="translate(284 14)">
      <circle cx="9" cy="8" r="3" fill="none" stroke="#E5E7EB" stroke-width="1.4"/>
      <path d="M9 1 L9 3 M9 13 L9 15 M2 8 L4 8 M14 8 L16 8 M4 3 L5.5 4.5 M12.5 11.5 L14 13 M4 13 L5.5 11.5 M12.5 4.5 L14 3" stroke="#E5E7EB" stroke-width="1.4" stroke-linecap="round"/>
    </g>
  </g>

  <!-- Outer 1px Border -->
  <rect width="320" height="330" rx="12" fill="none" stroke="#2C2C2C" stroke-width="1"/>
</g>
</svg>`;
}

// Write SVGs
fs.writeFileSync(path.join(outDir, 'screenshot-dock.svg'), buildScreenDock());
fs.writeFileSync(path.join(outDir, 'pult-hero-dock.svg'), buildScreenDock());
fs.writeFileSync(path.join(outDir, 'screenshot-touchpad.svg'), buildScreenTouchpad());
fs.writeFileSync(path.join(outDir, 'screenshot-clipboard.svg'), buildScreenClipboard());
fs.writeFileSync(path.join(outDir, 'screenshot-signature.svg'), buildScreenSignature());
fs.writeFileSync(path.join(outDir, 'screenshot-capture.svg'), buildScreenCapture());
fs.writeFileSync(path.join(outDir, 'windows-tray-companion.svg'), buildWindowsTrayFlyout());

console.log('Successfully written all SVGs in English with valid XML!');
