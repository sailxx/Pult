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

const chromeIco = getBase64Icon('chrome.png');
const braveIco = getBase64Icon('brave.png');
const vlcIco = getBase64Icon('vlc.png');
const explorerIco = getBase64Icon('explorer.png');
const cmdIco = getBase64Icon('cmd.png');

const STYLES = `
  text { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
  .mono { font-family: 'Consolas', 'Courier New', monospace; }
  .glass { fill: #1B2629; fill-opacity: 0.85; stroke: #354449; stroke-width: 1; }
  .glass-card { fill: #1B2629; fill-opacity: 0.88; stroke: #354449; stroke-width: 1; }
  .tile { fill: #1B2629; fill-opacity: 0.85; stroke: #354449; stroke-width: 1; }
  .tile-fav { fill: #202E33; fill-opacity: 0.95; stroke: #B4EDCC; stroke-opacity: 0.6; stroke-width: 1.5; }
  .tile-run { fill: #1B2629; fill-opacity: 0.90; stroke: #A3E6BE; stroke-opacity: 0.85; stroke-width: 1.5; }
  .btn { fill: #263438; stroke: #354449; stroke-width: 1; }
  .btn-accent { fill: #B4EDCC; }
  .badge { fill: #263438; stroke: #354449; stroke-width: 1; }
`;

function auroraBackground(w, h) {
  return `
    <defs>
      <radialGradient id="auroraA" cx="15%" cy="10%" r="55%">
        <stop offset="0%" stop-color="#71A78A" stop-opacity="0.50"/>
        <stop offset="60%" stop-color="#71A78A" stop-opacity="0.12"/>
        <stop offset="100%" stop-color="#71A78A" stop-opacity="0"/>
      </radialGradient>
      <radialGradient id="auroraB" cx="82%" cy="88%" r="52%">
        <stop offset="0%" stop-color="#274268" stop-opacity="0.48"/>
        <stop offset="60%" stop-color="#274268" stop-opacity="0.10"/>
        <stop offset="100%" stop-color="#274268" stop-opacity="0"/>
      </radialGradient>
      <radialGradient id="auroraC" cx="65%" cy="25%" r="42%">
        <stop offset="0%" stop-color="#273F45" stop-opacity="0.38"/>
        <stop offset="100%" stop-color="#273F45" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <rect width="${w}" height="${h}" fill="#101719"/>
    <rect width="${w}" height="${h}" fill="url(#auroraA)"/>
    <rect width="${w}" height="${h}" fill="url(#auroraB)"/>
    <rect width="${w}" height="${h}" fill="url(#auroraC)"/>
  `;
}

function headerBar(activeTab = 'Док') {
  const tabs = ['Док', 'Тачпад', 'Буфер', 'Подпись', 'Снимок', 'Программы'];
  let tabX = 3;
  let tabsSvg = '';

  for (const t of tabs) {
    const isActive = t === activeTab;
    const tw = t.length > 5 ? 88 : 72;
    if (isActive) {
      tabsSvg += `
        <rect x="${tabX}" y="3" width="${tw}" height="28" rx="8" fill="#263438" stroke="#B4EDCC" stroke-opacity="0.5"/>
        <text x="${tabX + tw/2}" y="21" fill="#B4EDCC" font-size="11" font-weight="800" text-anchor="middle">${t}</text>
      `;
    } else {
      tabsSvg += `
        <text x="${tabX + tw/2}" y="21" fill="#A0AFAF" font-size="11" font-weight="600" text-anchor="middle">${t}</text>
      `;
    }
    tabX += tw + 4;
  }

  return `
    <!-- Top 44dp Header Bar -->
    <g transform="translate(24 16)">
      <!-- Left: Host status -->
      <rect x="0" y="0" width="180" height="34" rx="10" class="glass"/>
      <circle cx="16" cy="17" r="3.5" fill="#A3E6BE"/>
      <text x="28" y="21" fill="#EEF3F1" font-size="11.5" font-weight="700">DESKTOP-6T5OKAJ</text>
      <text x="148" y="21" fill="#758585" font-size="10" class="mono">LAN</text>

      <!-- Center: Segmented Navigation -->
      <g transform="translate(320 0)">
        <rect x="0" y="0" width="${tabX + 2}" height="34" rx="10" class="glass"/>
        ${tabsSvg}
      </g>

      <!-- Right: Settings and Disconnect -->
      <g transform="translate(1148 0)">
        <rect x="0" y="0" width="34" height="34" rx="10" class="glass"/>
        <text x="17" y="22" fill="#A0AFAF" font-size="14" text-anchor="middle">⚙</text>

        <rect x="42" y="0" width="34" height="34" rx="10" class="glass"/>
        <text x="59" y="21" fill="#A0AFAF" font-size="12" font-weight="700" text-anchor="middle">✕</text>
      </g>
    </g>
  `;
}

// 1. DOCK & APPS SCREEN
function buildScreenDock() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Landscape Dock">
<style>${STYLES}</style>
${auroraBackground(1280, 720)}
${headerBar('Док')}

<!-- Dock Sub-header -->
<g transform="translate(30 76)">
  <text x="0" y="16" fill="#758585" font-size="11" font-weight="800" letter-spacing="0.6">ГЛАВНЫЙ ДОК</text>
  <rect x="110" y="2" width="100" height="20" rx="6" fill="#B4EDCC" fill-opacity="0.12" stroke="#B4EDCC" stroke-opacity="0.3"/>
  <text x="160" y="15" fill="#B4EDCC" font-size="10" font-weight="700" text-anchor="middle">★ 6 в фокусе</text>

  <g transform="translate(1040 0)">
    <rect x="0" y="0" width="180" height="26" rx="8" class="glass"/>
    <text x="90" y="17" fill="#B4EDCC" font-size="11" font-weight="600" text-anchor="middle">Все программы (42) →</text>
  </g>
</g>

<!-- 6 Large Dock Tiles -->
<g transform="translate(100 160)">
  <!-- Tile 1: Brave Browser (Real EXE icon) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="144" height="144" class="tile-fav"/>
    <image href="${braveIco}" x="38" y="28" width="68" height="68"/>
    <circle cx="72" cy="130" r="3.5" fill="#A3E6BE"/>
    <text x="72" y="176" fill="#EEF3F1" font-size="13" font-weight="700" text-anchor="middle">Brave</text>
  </g>

  <!-- Tile 2: Google Chrome (Real EXE icon) -->
  <g transform="translate(188 0)">
    <rect x="0" y="0" width="144" height="144" class="tile-run"/>
    <image href="${chromeIco}" x="38" y="28" width="68" height="68"/>
    <circle cx="72" cy="130" r="3.5" fill="#A3E6BE"/>
    <text x="72" y="176" fill="#EEF3F1" font-size="13" font-weight="700" text-anchor="middle">Chrome</text>
  </g>

  <!-- Tile 3: VS Code -->
  <g transform="translate(376 0)">
    <rect x="0" y="0" width="144" height="144" class="tile-fav"/>
    <!-- Realistic VS Code vector path -->
    <g transform="translate(40 28) scale(1.4)">
      <path d="M33 46 L45 36 L45 12 L33 2 Z" fill="#0065A9"/>
      <path d="M33 46 L14 31 L3 40 L1 38 L1 10 L3 8 L14 17 L33 2 Z" fill="#007ACC"/>
      <path d="M33 34 L18 24 L33 14 Z" fill="#1F9CF0"/>
    </g>
    <circle cx="72" cy="130" r="3.5" fill="#A3E6BE"/>
    <text x="72" y="176" fill="#EEF3F1" font-size="13" font-weight="700" text-anchor="middle">VS Code</text>
  </g>

  <!-- Tile 4: VLC Media Player (Real EXE icon) -->
  <g transform="translate(564 0)">
    <rect x="0" y="0" width="144" height="144" class="tile"/>
    <image href="${vlcIco}" x="38" y="28" width="68" height="68"/>
    <text x="72" y="176" fill="#A0AFAF" font-size="13" font-weight="600" text-anchor="middle">VLC Player</text>
  </g>

  <!-- Tile 5: File Explorer (Real EXE icon) -->
  <g transform="translate(752 0)">
    <rect x="0" y="0" width="144" height="144" class="tile-run"/>
    <image href="${explorerIco}" x="38" y="28" width="68" height="68"/>
    <circle cx="72" cy="130" r="3.5" fill="#A3E6BE"/>
    <text x="72" y="176" fill="#EEF3F1" font-size="13" font-weight="700" text-anchor="middle">Проводник</text>
  </g>

  <!-- Tile 6: Windows Terminal (Real EXE icon) -->
  <g transform="translate(940 0)">
    <rect x="0" y="0" width="144" height="144" class="tile"/>
    <image href="${cmdIco}" x="38" y="28" width="68" height="68"/>
    <text x="72" y="176" fill="#A0AFAF" font-size="13" font-weight="600" text-anchor="middle">Терминал</text>
  </g>
</g>

<!-- NOW PLAYING & VOLUME BOTTOM BAR -->
<g transform="translate(24 590)">
  <rect x="0" y="0" width="1232" height="96" rx="18" class="glass"/>

  <!-- Volume Group -->
  <g transform="translate(24 24)">
    <rect x="0" y="0" width="88" height="48" rx="10" fill="#263438" stroke="#354449"/>
    <text x="44" y="30" fill="#B4EDCC" font-size="14" font-weight="800" text-anchor="middle">70%</text>

    <rect x="100" y="0" width="46" height="48" rx="10" fill="#263438" stroke="#354449"/>
    <text x="123" y="31" fill="#EEF3F1" font-size="18" font-weight="700" text-anchor="middle">−</text>

    <rect x="154" y="0" width="46" height="48" rx="10" fill="#263438" stroke="#354449"/>
    <text x="177" y="31" fill="#EEF3F1" font-size="18" font-weight="700" text-anchor="middle">+</text>
  </g>

  <!-- Center Media Controls -->
  <g transform="translate(540 22)">
    <!-- Prev -->
    <circle cx="26" cy="26" r="22" fill="#263438" stroke="#354449"/>
    <path d="M30 18 L20 26 L30 34 M18 18 L18 34" stroke="#EEF3F1" stroke-width="2.5" stroke-linecap="round" fill="none"/>

    <!-- Play/Pause -->
    <circle cx="86" cy="26" r="28" fill="#B4EDCC"/>
    <path d="M82 17 L95 26 L82 35 Z" fill="#101719"/>

    <!-- Next -->
    <circle cx="146" cy="26" r="22" fill="#263438" stroke="#354449"/>
    <path d="M142 18 L152 26 L142 34 M154 18 L154 34" stroke="#EEF3F1" stroke-width="2.5" stroke-linecap="round" fill="none"/>
  </g>

  <!-- Right Actions: Desktop & Lock -->
  <g transform="translate(970 24)">
    <rect x="0" y="0" width="130" height="48" rx="10" fill="#263438" stroke="#354449"/>
    <text x="65" y="29" fill="#A0AFAF" font-size="12" font-weight="600" text-anchor="middle">Рабочий стол</text>

    <rect x="142" y="0" width="100" height="48" rx="10" fill="#3D2024" stroke="#EF4444" stroke-opacity="0.4"/>
    <text x="192" y="29" fill="#EF4444" font-size="12" font-weight="700" text-anchor="middle">Блок ПК</text>
  </g>
</g>
</svg>`;
}

// 2. TOUCHPAD SCREEN
function buildScreenTouchpad() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Touchpad">
<style>${STYLES}</style>
${auroraBackground(1280, 720)}
${headerBar('Тачпад')}

<g transform="translate(24 76)">
  <!-- Left: Main Touchpad Card (78% width = 940px) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="940" height="610" rx="16" class="glass"/>

    <!-- Subheader inside touchpad -->
    <g transform="translate(20 18)">
      <circle cx="6" cy="6" r="4" fill="#B4EDCC"/>
      <text x="18" y="10" fill="#758585" font-size="11" font-weight="800" letter-spacing="0.5">ТАЧПАД 60 FPS</text>

      <!-- Speed slider -->
      <g transform="translate(640 -6)">
        <text x="0" y="16" fill="#A0AFAF" font-size="11" font-weight="600">Скорость 1.6×</text>
        <rect x="90" y="10" width="150" height="4" rx="2" fill="#354449"/>
        <rect x="90" y="10" width="85" height="4" rx="2" fill="#B4EDCC"/>
        <circle cx="175" cy="12" r="7" fill="#B4EDCC"/>
      </g>
    </g>

    <!-- Glass Touch Area -->
    <g transform="translate(20 52)">
      <rect x="0" y="0" width="900" height="450" rx="14" fill="#131B1E" stroke="#2B3A3F" stroke-width="1.5"/>
      <circle cx="450" cy="225" r="54" stroke="#354449" stroke-width="1.5" stroke-dasharray="4 4" fill="none"/>
      <circle cx="450" cy="225" r="5" fill="#B4EDCC" fill-opacity="0.8"/>
      <text x="450" y="270" fill="#758585" font-size="12" font-weight="500" text-anchor="middle">Перемещайте курсор пальцем</text>
      <text x="450" y="292" fill="#586A6F" font-size="11" text-anchor="middle">Тап — ЛКМ  •  2× Тап — Двойной клик  •  Удержание — ПКМ</text>
    </g>

    <!-- Bottom Hardware Buttons -->
    <g transform="translate(20 520)">
      <!-- LKM -->
      <rect x="0" y="0" width="370" height="68" rx="12" fill="#202F33" stroke="#354449"/>
      <text x="185" y="40" fill="#EEF3F1" font-size="15" font-weight="800" text-anchor="middle">ЛКМ</text>

      <!-- Drag & Drop Lock -->
      <rect x="390" y="0" width="160" height="68" rx="12" fill="#202F33" stroke="#354449"/>
      <text x="470" y="34" fill="#B4EDCC" font-size="12" font-weight="800" text-anchor="middle">Drag &amp; Drop</text>
      <text x="470" y="50" fill="#758585" font-size="10" text-anchor="middle">Режим удержания</text>

      <!-- SCM -->
      <rect x="570" y="0" width="150" height="68" rx="12" fill="#202F33" stroke="#354449"/>
      <text x="645" y="40" fill="#A0AFAF" font-size="13" font-weight="700" text-anchor="middle">СКМ</text>

      <!-- RKM -->
      <rect x="740" y="0" width="160" height="68" rx="12" fill="#202F33" stroke="#354449"/>
      <text x="820" y="40" fill="#EEF3F1" font-size="15" font-weight="800" text-anchor="middle">ПКМ</text>
    </g>
  </g>

  <!-- Right: Scroll Wheel Card (22% width = 270px) -->
  <g transform="translate(962 0)">
    <rect x="0" y="0" width="270" height="610" rx="16" class="glass"/>
    <g transform="translate(135 300)">
      <text x="0" y="-80" fill="#B4EDCC" font-size="20" text-anchor="middle">▲</text>
      <text x="0" y="-30" fill="#EEF3F1" font-size="13" font-weight="800" text-anchor="middle" letter-spacing="1">КОЛЕСО</text>
      <text x="0" y="-12" fill="#EEF3F1" font-size="13" font-weight="800" text-anchor="middle" letter-spacing="1">СКРОЛЛА</text>
      <text x="0" y="30" fill="#B4EDCC" font-size="20" text-anchor="middle">▼</text>
      <text x="0" y="80" fill="#758585" font-size="11" text-anchor="middle">Вверх / Вниз для</text>
      <text x="0" y="96" fill="#758585" font-size="11" text-anchor="middle">плавной прокрутки</text>
    </g>
  </g>
</g>
</svg>`;
}

// 3. CLIPBOARD SCREEN
function buildScreenClipboard() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Clipboard">
<style>${STYLES}</style>
${auroraBackground(1280, 720)}
${headerBar('Буфер')}

<g transform="translate(24 76)">
  <!-- Left Column: Windows Clipboard (670px) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="670" height="610" rx="16" class="glass"/>
    <g transform="translate(24 20)">
      <circle cx="6" cy="8" r="4" fill="#B4EDCC"/>
      <text x="18" y="12" fill="#758585" font-size="11" font-weight="800" letter-spacing="0.5">БУФЕР ОБМЕНА WINDOWS</text>

      <rect x="520" y="-4" width="90" height="28" rx="8" fill="#263438" stroke="#354449"/>
      <text x="565" y="14" fill="#EEF3F1" font-size="11" font-weight="600" text-anchor="middle">Обновить</text>
    </g>

    <!-- Clipboard content box -->
    <g transform="translate(24 64)">
      <rect x="0" y="0" width="622" height="440" rx="12" fill="#131B1E" stroke="#2B3A3F"/>
      <text x="20" y="36" fill="#B4EDCC" font-size="13" class="mono font-weight-700">git clone https://github.com/sailxx/Pult.git</text>
      <text x="20" y="64" fill="#A0AFAF" font-size="13" class="mono">cd pult &amp;&amp; host\\build.cmd</text>
      <text x="20" y="100" fill="#758585" font-size="12" class="mono">// Pult Windows Host: Peer-to-peer keep-alive latency 1.04ms</text>
    </g>

    <!-- Actions -->
    <g transform="translate(24 524)">
      <rect x="0" y="0" width="300" height="54" rx="12" fill="#263438" stroke="#354449"/>
      <text x="150" y="32" fill="#B4EDCC" font-size="13" font-weight="700" text-anchor="middle">В буфер телефона</text>

      <rect x="322" y="0" width="300" height="54" rx="12" fill="#3D2024" stroke="#EF4444" stroke-opacity="0.4"/>
      <text x="472" y="32" fill="#EF4444" font-size="13" font-weight="700" text-anchor="middle">Очистить буфер ПК</text>
    </g>
  </g>

  <!-- Right Column: Send to PC (540px) -->
  <g transform="translate(692 0)">
    <rect x="0" y="0" width="540" height="610" rx="16" class="glass"/>
    <g transform="translate(24 20)">
      <circle cx="6" cy="8" r="4" fill="#A3E6BE"/>
      <text x="18" y="12" fill="#758585" font-size="11" font-weight="800" letter-spacing="0.5">ОТПРАВИТЬ НА ПК</text>

      <rect x="330" y="-4" width="160" height="28" rx="8" fill="#263438" stroke="#354449"/>
      <text x="410" y="14" fill="#B4EDCC" font-size="11" font-weight="600" text-anchor="middle">Вставить из телефона</text>
    </g>

    <!-- Input text area -->
    <g transform="translate(24 64)">
      <rect x="0" y="0" width="492" height="440" rx="12" fill="#131B1E" stroke="#2B3A3F"/>
      <text x="20" y="36" fill="#EEF3F1" font-size="13" class="mono">https://windock.vercel.app/</text>
      <rect x="235" y="22" width="2" height="18" fill="#B4EDCC"/>
    </g>

    <!-- Send button -->
    <g transform="translate(24 524)">
      <rect x="0" y="0" width="492" height="54" rx="12" fill="#B4EDCC"/>
      <text x="246" y="33" fill="#101719" font-size="14" font-weight="800" text-anchor="middle">Скопировать в буфер ПК</text>
    </g>
  </g>
</g>
</svg>`;
}

// 4. SIGNATURE SCREEN
function buildScreenSignature() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720" role="img" aria-label="Pult Signature">
<style>${STYLES}</style>
${auroraBackground(1280, 720)}
${headerBar('Подпись')}

<g transform="translate(24 76)">
  <!-- Left: Signature Canvas (940px) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="940" height="610" rx="16" class="glass"/>

    <g transform="translate(20 18)">
      <circle cx="6" cy="6" r="4" fill="#B4EDCC"/>
      <text x="18" y="10" fill="#758585" font-size="11" font-weight="800" letter-spacing="0.5">ХОЛСТ ДЛЯ ПОДПИСИ И НАБРОСКОВ</text>

      <g transform="translate(730 -6)">
        <rect x="0" y="0" width="85" height="28" rx="8" fill="#263438" stroke="#354449"/>
        <text x="42" y="18" fill="#EEF3F1" font-size="11" font-weight="600" text-anchor="middle">Отменить</text>

        <rect x="95" y="0" width="85" height="28" rx="8" fill="#3D2024" stroke="#EF4444" stroke-opacity="0.4"/>
        <text x="137" y="18" fill="#EF4444" font-size="11" font-weight="600" text-anchor="middle">Очистить</text>
      </g>
    </g>

    <!-- Canvas surface -->
    <g transform="translate(20 52)">
      <rect x="0" y="0" width="900" height="536" rx="14" fill="#131B1E" stroke="#2B3A3F" stroke-width="1.5"/>
      <line x1="60" y1="420" x2="840" y2="420" stroke="#354449" stroke-width="1.5"/>

      <!-- Smooth Realistic Signature Stroke -->
      <path d="M 120 380 Q 180 200 240 370 Q 280 430 320 280 T 400 370 Q 480 250 560 390 Q 640 460 740 310" 
            fill="none" stroke="#FFFFFF" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M 230 330 Q 380 320 540 335" 
            fill="none" stroke="#FFFFFF" stroke-width="4.5" stroke-linecap="round"/>
    </g>
  </g>

  <!-- Right: Palette & Action Buttons (270px) -->
  <g transform="translate(962 0)">
    <rect x="0" y="0" width="270" height="610" rx="16" class="glass"/>

    <g transform="translate(24 24)">
      <text x="0" y="16" fill="#758585" font-size="11" font-weight="800">ЦВЕТ ПЕРА</text>
      <g transform="translate(0 32)">
        <circle cx="20" cy="20" r="16" fill="#FFFFFF" stroke="#B4EDCC" stroke-width="2.5"/>
        <circle cx="68" cy="20" r="14" fill="#38BDF8"/>
        <circle cx="116" cy="20" r="14" fill="#22C55E"/>
        <circle cx="164" cy="20" r="14" fill="#F59E0B"/>
        <circle cx="212" cy="20" r="14" fill="#101719" stroke="#354449" stroke-width="2"/>
      </g>

      <text x="0" y="104" fill="#758585" font-size="11" font-weight="800">ТОЛЩИНА</text>
      <g transform="translate(0 120)">
        <rect x="0" y="0" width="70" height="34" rx="8" fill="#202F33" stroke="#354449"/>
        <text x="35" y="21" fill="#A0AFAF" font-size="11" text-anchor="middle">Тонко</text>

        <rect x="76" y="0" width="70" height="34" rx="8" fill="#263438" stroke="#B4EDCC"/>
        <text x="111" y="21" fill="#B4EDCC" font-size="11" font-weight="700" text-anchor="middle">Средне</text>

        <rect x="152" y="0" width="70" height="34" rx="8" fill="#202F33" stroke="#354449"/>
        <text x="187" y="21" fill="#A0AFAF" font-size="11" text-anchor="middle">Жирно</text>
      </g>

      <!-- Insertion actions -->
      <g transform="translate(0 380)">
        <rect x="0" y="0" width="222" height="58" rx="12" fill="#B4EDCC"/>
        <text x="111" y="34" fill="#101719" font-size="13" font-weight="800" text-anchor="middle">Вставить на ПК (Ctrl+V)</text>

        <rect x="0" y="70" width="222" height="52" rx="12" fill="#263438" stroke="#354449"/>
        <text x="111" y="102" fill="#EEF3F1" font-size="12" font-weight="700" text-anchor="middle">Сохранить на ПК</text>
        <text x="111" y="126" fill="#758585" font-size="9" text-anchor="middle">Pictures\\Pult\\Signatures</text>
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
${auroraBackground(1280, 720)}
${headerBar('Снимок')}

<g transform="translate(24 76)">
  <!-- Left: Screenshot Preview (940px) -->
  <g transform="translate(0 0)">
    <rect x="0" y="0" width="940" height="610" rx="16" class="glass"/>

    <g transform="translate(20 18)">
      <circle cx="6" cy="6" r="4" fill="#A3E6BE"/>
      <text x="18" y="10" fill="#758585" font-size="11" font-weight="800" letter-spacing="0.5">СНИМОК ЭКРАНА WINDOWS</text>
      <text x="780" y="10" fill="#A0AFAF" font-size="11" font-weight="600">Время: 12:00:24</text>
    </g>

    <!-- Realistic Windows Desktop Screenshot Preview -->
    <g transform="translate(20 52)">
      <rect x="0" y="0" width="900" height="536" rx="14" fill="#0C1217" stroke="#2B3A3F" stroke-width="1.5"/>
      <!-- Desktop wallpaper graphic -->
      <rect x="2" y="2" width="896" height="490" fill="#18232C"/>
      <circle cx="300" cy="200" r="160" fill="#253A47" fill-opacity="0.4"/>
      <circle cx="600" cy="300" r="220" fill="#1E323E" fill-opacity="0.5"/>

      <!-- Window 1: VS Code Editor -->
      <g transform="translate(80 50)">
        <rect x="0" y="0" width="540" height="340" rx="10" fill="#1E1E1E" stroke="#333333"/>
        <rect x="0" y="0" width="540" height="32" rx="10" fill="#2D2D2D"/>
        <circle cx="20" cy="16" r="5" fill="#EF4444"/>
        <circle cx="36" cy="16" r="5" fill="#F59E0B"/>
        <circle cx="52" cy="16" r="5" fill="#22C55E"/>
        <text x="270" y="20" fill="#A0AFAF" font-size="11" text-anchor="middle">Pult.cs — Visual Studio Code</text>

        <!-- Code lines -->
        <text x="24" y="66" fill="#569CD6" font-size="12" class="mono">namespace <tspan fill="#4EC9B0">Pult</tspan> {</text>
        <text x="44" y="92" fill="#569CD6" font-size="12" class="mono">public class <tspan fill="#4EC9B0">Server</tspan> {</text>
        <text x="64" y="118" fill="#DCDCAA" font-size="12" class="mono">StartServer<tspan fill="#D4D4D4">();</tspan></text>
        <text x="64" y="144" fill="#6A9955" font-size="12" class="mono">// Latency: 1.04ms</text>
        <text x="44" y="170" fill="#D4D4D4" font-size="12" class="mono">}</text>
        <text x="24" y="196" fill="#D4D4D4" font-size="12" class="mono">}</text>
      </g>

      <!-- Windows Taskbar -->
      <g transform="translate(0 492)">
        <rect x="2" y="0" width="896" height="42" fill="#10171D"/>
        <!-- Start icon -->
        <rect x="420" y="12" width="18" height="18" fill="#38BDF8" rx="2"/>
        <circle cx="460" cy="21" r="10" fill="#2563EB"/>
        <circle cx="490" cy="21" r="10" fill="#10B981"/>
      </g>
    </g>
  </g>

  <!-- Right: Actions (270px) -->
  <g transform="translate(962 0)">
    <rect x="0" y="0" width="270" height="610" rx="16" class="glass"/>

    <g transform="translate(24 24)">
      <text x="0" y="16" fill="#758585" font-size="11" font-weight="800">ДЕЙСТВИЯ</text>

      <g transform="translate(0 40)">
        <rect x="0" y="0" width="222" height="54" rx="12" fill="#B4EDCC"/>
        <text x="111" y="32" fill="#101719" font-size="13" font-weight="800" text-anchor="middle">Сделать снимок</text>

        <rect x="0" y="66" width="222" height="50" rx="12" fill="#263438" stroke="#354449"/>
        <text x="111" y="97" fill="#EEF3F1" font-size="12" font-weight="700" text-anchor="middle">В буфер ПК</text>

        <rect x="0" y="126" width="222" height="50" rx="12" fill="#263438" stroke="#354449"/>
        <text x="111" y="157" fill="#EEF3F1" font-size="12" font-weight="700" text-anchor="middle">Сохранить на ПК</text>
        <text x="111" y="196" fill="#758585" font-size="10" text-anchor="middle">Pictures\\Pult</text>
      </g>
    </g>
  </g>
</g>
</svg>`;
}

fs.writeFileSync(path.join(outDir, 'screenshot-dock.svg'), buildScreenDock());
fs.writeFileSync(path.join(outDir, 'screenshot-touchpad.svg'), buildScreenTouchpad());
fs.writeFileSync(path.join(outDir, 'screenshot-clipboard.svg'), buildScreenClipboard());
fs.writeFileSync(path.join(outDir, 'screenshot-signature.svg'), buildScreenSignature());
fs.writeFileSync(path.join(outDir, 'screenshot-capture.svg'), buildScreenCapture());

// Also update the hero dock SVG to use real icons!
fs.writeFileSync(path.join(outDir, 'pult-hero-dock.svg'), buildScreenDock());

console.log('Successfully generated all accurate Android screens!');
