import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const outDir = path.resolve(here, '../assets/screenshots');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

// Common SVG Defs & Styles
const STYLES = `
  @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap');
  text { font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
  .mono { font-family: 'JetBrains Mono', monospace; }
  .glass { fill: #1B2629; fill-opacity: 0.82; stroke: #354449; stroke-width: 1; }
  .glass-card { fill: #172124; fill-opacity: 0.88; stroke: #2D3A3F; stroke-width: 1; rx: 14; }
  .tile { fill: #1D2A2E; stroke: #38494F; stroke-width: 1; rx: 14; }
  .tile-fav { fill: #23343A; stroke: #B4EDCC; stroke-opacity: 0.55; stroke-width: 1.5; rx: 14; }
  .tile-run { fill: #1D2A2E; stroke: #A3E6BE; stroke-opacity: 0.85; stroke-width: 1.5; rx: 14; }
  .btn { fill: #263438; stroke: #3B4D53; stroke-width: 1; rx: 8; }
  .btn-accent { fill: #B4EDCC; rx: 8; }
  .badge { fill: #263438; stroke: #3B4D53; stroke-width: 1; rx: 6; }
`;

function buildHeroDockSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="740" viewBox="0 0 1280 740" role="img" aria-label="Pult Landscape Dock Hero">
<defs>
  <style>${STYLES}</style>
  <radialGradient id="auroraA" cx="15%" cy="10%" r="55%">
    <stop offset="0%" stop-color="#71A78A" stop-opacity="0.45"/>
    <stop offset="60%" stop-color="#71A78A" stop-opacity="0.10"/>
    <stop offset="100%" stop-color="#71A78A" stop-opacity="0"/>
  </radialGradient>
  <radialGradient id="auroraB" cx="82%" cy="88%" r="50%">
    <stop offset="0%" stop-color="#274268" stop-opacity="0.50"/>
    <stop offset="60%" stop-color="#274268" stop-opacity="0.12"/>
    <stop offset="100%" stop-color="#274268" stop-opacity="0"/>
  </radialGradient>
  <radialGradient id="auroraC" cx="65%" cy="25%" r="40%">
    <stop offset="0%" stop-color="#273F45" stop-opacity="0.40"/>
    <stop offset="100%" stop-color="#273F45" stop-opacity="0"/>
  </radialGradient>
  <clipPath id="phoneClip"><rect x="140" y="40" width="1000" height="660" rx="36"/></clipPath>
</defs>

<!-- Deep OLED Background Canvas -->
<rect width="1280" height="740" fill="#0A0D0E"/>

<!-- Phone Frame in Landscape Orientation on Desk Stand -->
<g>
  <!-- Outer Phone Bezel -->
  <rect x="134" y="34" width="1012" height="672" rx="42" fill="#14181A" stroke="#252F33" stroke-width="2"/>
  <rect x="138" y="38" width="1004" height="664" rx="38" fill="#0E1214"/>
  
  <!-- Left punch-hole camera -->
  <circle cx="158" cy="370" r="5" fill="#050708" stroke="#1D2427" stroke-width="2"/>

  <!-- Screen Content clipped to display corners -->
  <g clip-path="url(#phoneClip)">
    <!-- Ambient Aurora Wallpaper -->
    <rect x="140" y="40" width="1000" height="660" fill="#101719"/>
    <rect x="140" y="40" width="1000" height="660" fill="url(#auroraA)"/>
    <rect x="140" y="40" width="1000" height="660" fill="url(#auroraB)"/>
    <rect x="140" y="40" width="1000" height="660" fill="url(#auroraC)"/>

    <!-- PULT HEADER (44dp equivalent) -->
    <g transform="translate(164 56)">
      <!-- Host name status card -->
      <rect x="0" y="0" width="170" height="38" rx="10" class="glass"/>
      <circle cx="16" cy="19" r="4" fill="#A3E6BE"/>
      <text x="28" y="23" fill="#EEF3F1" font-size="12" font-weight="700">DESKTOP-WIN11</text>
      <text x="135" y="23" fill="#758585" font-size="10" class="mono">LAN</text>

      <!-- Segmented Navigation Pills -->
      <g transform="translate(230 0)">
        <rect x="0" y="0" width="480" height="38" rx="10" class="glass"/>
        <!-- Active: Док -->
        <rect x="3" y="3" width="76" height="32" rx="8" fill="#263438" stroke="#B4EDCC" stroke-opacity="0.5"/>
        <text x="41" y="23" fill="#B4EDCC" font-size="11" font-weight="800" text-anchor="middle">Док</text>

        <text x="122" y="23" fill="#A0AFAF" font-size="11" font-weight="600" text-anchor="middle">Тачпад</text>
        <text x="202" y="23" fill="#A0AFAF" font-size="11" font-weight="600" text-anchor="middle">Буфер</text>
        <text x="282" y="23" fill="#A0AFAF" font-size="11" font-weight="600" text-anchor="middle">Подпись</text>
        <text x="362" y="23" fill="#A0AFAF" font-size="11" font-weight="600" text-anchor="middle">Снимок</text>
        <text x="438" y="23" fill="#A0AFAF" font-size="11" font-weight="600" text-anchor="middle">Программы</text>
      </g>

      <!-- Right Actions: Settings Cog & Disconnect -->
      <g transform="translate(860 0)">
        <rect x="0" y="0" width="38" height="38" rx="10" class="glass"/>
        <text x="19" y="24" fill="#A0AFAF" font-size="15" text-anchor="middle">⚙</text>

        <rect x="46" y="0" width="38" height="38" rx="10" class="glass"/>
        <text x="65" y="24" fill="#A0AFAF" font-size="13" font-weight="700" text-anchor="middle">✕</text>
      </g>
    </g>

    <!-- DOCK SUB-HEADER: Title + All apps pill -->
    <g transform="translate(170 114)">
      <text x="0" y="16" fill="#758585" font-size="11" font-weight="800" letter-spacing="0.8">ГЛАВНЫЙ ДОК</text>
      <rect x="110" y="2" width="100" height="20" rx="6" fill="#B4EDCC" fill-opacity="0.12" stroke="#B4EDCC" stroke-opacity="0.3"/>
      <text x="160" y="15" fill="#B4EDCC" font-size="10" font-weight="700" text-anchor="middle">★ 6 в фокусе</text>

      <!-- Button All Programs -->
      <g transform="translate(790 0)">
        <rect x="0" y="0" width="160" height="26" rx="8" class="glass"/>
        <text x="80" y="17" fill="#B4EDCC" font-size="11" font-weight="600" text-anchor="middle">Все программы (42) →</text>
      </g>
    </g>

    <!-- HERO DOCK GRID (6 Large Glass Tiles) -->
    <g transform="translate(196 170)">
      <!-- 1. VS Code -->
      <g transform="translate(0 0)">
        <rect x="0" y="0" width="128" height="128" class="tile-fav"/>
        <!-- VS Code Blue Icon Graphic -->
        <rect x="36" y="28" width="56" height="56" rx="12" fill="#007ACC"/>
        <path d="M52 46 L68 56 L52 66 Z" fill="#FFFFFF"/>
        <circle cx="64" cy="116" r="3.5" fill="#A3E6BE"/>
        <text x="64" y="154" fill="#EEF3F1" font-size="12" font-weight="700" text-anchor="middle">VS Code</text>
      </g>

      <!-- 2. Google Chrome -->
      <g transform="translate(164 0)">
        <rect x="0" y="0" width="128" height="128" class="tile-run"/>
        <circle cx="64" cy="56" r="28" fill="#EA4335"/>
        <circle cx="64" cy="56" r="16" fill="#FBBC05"/>
        <circle cx="64" cy="56" r="10" fill="#4285F4"/>
        <circle cx="64" cy="116" r="3.5" fill="#A3E6BE"/>
        <text x="64" y="154" fill="#EEF3F1" font-size="12" font-weight="700" text-anchor="middle">Chrome</text>
      </g>

      <!-- 3. Spotify -->
      <g transform="translate(328 0)">
        <rect x="0" y="0" width="128" height="128" class="tile-fav"/>
        <circle cx="64" cy="56" r="28" fill="#1ED760"/>
        <path d="M48 50 Q64 45 80 52 M51 58 Q64 54 77 60 M54 66 Q64 63 74 67" stroke="#121212" stroke-width="3" stroke-linecap="round" fill="none"/>
        <circle cx="64" cy="116" r="3.5" fill="#A3E6BE"/>
        <text x="64" y="154" fill="#EEF3F1" font-size="12" font-weight="700" text-anchor="middle">Spotify</text>
      </g>

      <!-- 4. Steam -->
      <g transform="translate(492 0)">
        <rect x="0" y="0" width="128" height="128" class="tile"/>
        <circle cx="64" cy="56" r="28" fill="#171A21"/>
        <circle cx="64" cy="56" r="26" stroke="#2A475E" stroke-width="2" fill="none"/>
        <circle cx="58" cy="52" r="8" fill="#66C0F4"/>
        <circle cx="74" cy="64" r="5" fill="#C7D5E0"/>
        <text x="64" y="154" fill="#A0AFAF" font-size="12" font-weight="600" text-anchor="middle">Steam</text>
      </g>

      <!-- 5. Telegram -->
      <g transform="translate(656 0)">
        <rect x="0" y="0" width="128" height="128" class="tile-run"/>
        <circle cx="64" cy="56" r="28" fill="#2AABEE"/>
        <path d="M50 56 L78 44 L70 68 L62 60 L60 66 Z" fill="#FFFFFF"/>
        <circle cx="64" cy="116" r="3.5" fill="#A3E6BE"/>
        <text x="64" y="154" fill="#EEF3F1" font-size="12" font-weight="700" text-anchor="middle">Telegram</text>
      </g>

      <!-- 6. Windows Terminal -->
      <g transform="translate(820 0)">
        <rect x="0" y="0" width="128" height="128" class="tile"/>
        <rect x="36" y="28" width="56" height="56" rx="10" fill="#0C1014" stroke="#4F5D65" stroke-width="1.5"/>
        <text x="46" y="58" fill="#B4EDCC" font-size="18" class="mono">&gt;_</text>
        <text x="64" y="154" fill="#A0AFAF" font-size="12" font-weight="600" text-anchor="middle">Terminal</text>
      </g>
    </g>

    <!-- NOW PLAYING & VOLUME BOTTOM BAR -->
    <g transform="translate(164 570)">
      <rect x="0" y="0" width="952" height="74" rx="16" class="glass"/>

      <!-- Volume Card -->
      <g transform="translate(20 18)">
        <rect x="0" y="0" width="80" height="38" rx="8" fill="#263438" stroke="#3B4D53"/>
        <text x="40" y="24" fill="#B4EDCC" font-size="13" font-weight="800" text-anchor="middle">70%</text>

        <rect x="90" y="0" width="36" height="38" rx="8" fill="#263438" stroke="#3B4D53"/>
        <text x="108" y="24" fill="#EEF3F1" font-size="16" font-weight="700" text-anchor="middle">−</text>

        <rect x="134" y="0" width="36" height="38" rx="8" fill="#263438" stroke="#3B4D53"/>
        <text x="152" y="24" fill="#EEF3F1" font-size="16" font-weight="700" text-anchor="middle">+</text>
      </g>

      <!-- Media Player Controls (Center) -->
      <g transform="translate(420 16)">
        <!-- Prev -->
        <circle cx="20" cy="21" r="18" fill="#263438" stroke="#3B4D53"/>
        <path d="M24 14 L16 21 L24 28 M14 14 L14 28" stroke="#EEF3F1" stroke-width="2" stroke-linecap="round" fill="none"/>

        <!-- Play/Pause -->
        <circle cx="70" cy="21" r="23" fill="#B4EDCC"/>
        <path d="M66 14 L78 21 L66 28 Z" fill="#101719"/>

        <!-- Next -->
        <circle cx="120" cy="21" r="18" fill="#263438" stroke="#3B4D53"/>
        <path d="M116 14 L124 21 L116 28 M126 14 L126 28" stroke="#EEF3F1" stroke-width="2" stroke-linecap="round" fill="none"/>
      </g>

      <!-- Quick Windows Desktop Actions (Right) -->
      <g transform="translate(710 18)">
        <rect x="0" y="0" width="110" height="38" rx="8" fill="#263438" stroke="#3B4D53"/>
        <text x="55" y="24" fill="#A0AFAF" font-size="11" font-weight="600" text-anchor="middle">Рабочий стол</text>

        <rect x="120" y="0" width="94" height="38" rx="8" fill="#3D2024" stroke="#EF4444" stroke-opacity="0.4"/>
        <text x="167" y="24" fill="#EF4444" font-size="11" font-weight="700" text-anchor="middle">Блок ПК</text>
      </g>
    </g>
  </g>
</g>
</svg>`;
}

function buildSuiteShowcaseSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="840" viewBox="0 0 1280 840" role="img" aria-label="Pult Feature Suite">
<defs>
  <style>${STYLES}</style>
  <clipPath id="c1"><rect x="40" y="80" width="575" height="340" rx="20"/></clipPath>
  <clipPath id="c2"><rect x="665" y="80" width="575" height="340" rx="20"/></clipPath>
  <clipPath id="c3"><rect x="40" y="460" width="575" height="340" rx="20"/></clipPath>
  <clipPath id="c4"><rect x="665" y="460" width="575" height="340" rx="20"/></clipPath>
</defs>

<rect width="1280" height="840" fill="#080C0D"/>

<!-- TITLE -->
<text x="640" y="44" fill="#EEF3F1" font-size="18" font-weight="800" text-anchor="middle" letter-spacing="1">PULT WORKSPACE SUITE</text>
<text x="640" y="64" fill="#758585" font-size="11" font-weight="600" text-anchor="middle">WINDOCK-INSPIRED MULTI-TOOL DESK COMPANION FOR WINDOWS PC</text>

<!-- PANEL 1: CLIPBOARD SYNC -->
<g clip-path="url(#c1)">
  <rect x="40" y="80" width="575" height="340" fill="#171228"/>
  <rect x="40" y="80" width="575" height="340" stroke="#463A68" stroke-width="1.5" rx="20" fill="none"/>
  
  <text x="65" y="112" fill="#E7D4FF" font-size="12" font-weight="800">1. ДВУХСТОРОННИЙ БУФЕР ОБМЕНА</text>
  <text x="65" y="128" fill="#AF7FA0" font-size="10">Синхронизация текстов и ссылок ПК ↔ Смартфон без облака</text>

  <!-- Left Card: PC Clipboard -->
  <rect x="65" y="145" width="250" height="250" rx="12" fill="#231B38" stroke="#463A68"/>
  <text x="80" y="170" fill="#E7D4FF" font-size="10" font-weight="700">БУФЕР ОБМЕНА WINDOWS</text>
  <rect x="80" y="184" width="220" height="150" rx="8" fill="#130D20"/>
  <text x="92" y="210" fill="#EEF3F1" font-size="10" class="mono">git commit -m "feat: pult"</text>
  <text x="92" y="228" fill="#A0AFAF" font-size="10" class="mono">https://github.com/sailxx/Pult</text>
  <rect x="80" y="348" width="105" height="34" rx="6" fill="#31274C" stroke="#463A68"/>
  <text x="132" y="369" fill="#E7D4FF" font-size="10" font-weight="600" text-anchor="middle">В телефон</text>
  <rect x="195" y="348" width="105" height="34" rx="6" fill="#421C24" stroke="#EF4444" stroke-opacity="0.5"/>
  <text x="247" y="369" fill="#EF4444" font-size="10" font-weight="700" text-anchor="middle">Очистить ПК</text>

  <!-- Right Card: Send to PC -->
  <rect x="335" y="145" width="255" height="250" rx="12" fill="#231B38" stroke="#463A68"/>
  <text x="350" y="170" fill="#D1B3F3" font-size="10" font-weight="700">ОТПРАВИТЬ НА ПК</text>
  <rect x="350" y="184" width="225" height="150" rx="8" fill="#130D20"/>
  <text x="362" y="210" fill="#AF7FA0" font-size="10">Вставьте скопированный текст...</text>
  <rect x="350" y="348" width="225" height="34" rx="6" fill="#E7D4FF"/>
  <text x="462" y="369" fill="#171228" font-size="11" font-weight="800" text-anchor="middle">Скопировать в буфер ПК</text>
</g>

<!-- PANEL 2: PRECISION GLASS TOUCHPAD -->
<g clip-path="url(#c2)">
  <rect x="665" y="80" width="575" height="340" fill="#101719"/>
  <rect x="665" y="80" width="575" height="340" stroke="#354449" stroke-width="1.5" rx="20" fill="none"/>

  <text x="690" y="112" fill="#B4EDCC" font-size="12" font-weight="800">2. СТЕКЛЯННЫЙ ТАЧПАД 60 FPS</text>
  <text x="690" y="128" fill="#758585" font-size="10">Плавное движение, жесты, Drag &amp; Drop и аппаратные кнопки мыши</text>

  <!-- Touchpad surface -->
  <rect x="690" y="145" width="410" height="190" rx="14" fill="#162225" stroke="#2B3A3F"/>
  <circle cx="895" cy="240" r="38" stroke="#354449" stroke-dasharray="3 3" fill="none"/>
  <circle cx="895" cy="240" r="4" fill="#B4EDCC" fill-opacity="0.6"/>
  <text x="895" y="266" fill="#758585" font-size="10" text-anchor="middle">1 палец — курсор • 2 пальца — скролл</text>

  <!-- Speed control pill -->
  <rect x="960" y="155" width="130" height="24" rx="6" fill="#202F33"/>
  <text x="1025" y="171" fill="#B4EDCC" font-size="10" font-weight="700" text-anchor="middle">Скорость: 1.6×</text>

  <!-- Mouse Buttons -->
  <rect x="690" y="345" width="160" height="50" rx="10" fill="#202F33" stroke="#354449"/>
  <text x="770" y="375" fill="#EEF3F1" font-size="13" font-weight="800" text-anchor="middle">ЛКМ</text>

  <rect x="860" y="345" width="90" height="50" rx="10" fill="#202F33" stroke="#354449"/>
  <text x="905" y="370" fill="#B4EDCC" font-size="10" font-weight="700" text-anchor="middle">Drag&amp;Drop</text>
  <text x="905" y="384" fill="#758585" font-size="8" text-anchor="middle">Зажать</text>

  <rect x="960" y="345" width="60" height="50" rx="10" fill="#202F33" stroke="#354449"/>
  <text x="990" y="375" fill="#A0AFAF" font-size="11" font-weight="700" text-anchor="middle">СКМ</text>

  <rect x="1030" y="345" width="70" height="50" rx="10" fill="#202F33" stroke="#354449"/>
  <text x="1065" y="375" fill="#EEF3F1" font-size="13" font-weight="800" text-anchor="middle">ПКМ</text>

  <!-- Right Scroll Strip -->
  <rect x="1115" y="145" width="105" height="250" rx="14" fill="#162225" stroke="#2B3A3F"/>
  <text x="1167" y="240" fill="#B4EDCC" font-size="16" text-anchor="middle">▲</text>
  <text x="1167" y="270" fill="#A0AFAF" font-size="10" font-weight="800" text-anchor="middle">СКРОЛЛ</text>
  <text x="1167" y="295" fill="#B4EDCC" font-size="16" text-anchor="middle">▼</text>
</g>

<!-- PANEL 3: STYLUS & SIGNATURE -->
<g clip-path="url(#c3)">
  <rect x="40" y="460" width="575" height="340" fill="#221615"/>
  <rect x="40" y="460" width="575" height="340" stroke="#593F3C" stroke-width="1.5" rx="20" fill="none"/>

  <text x="65" y="492" fill="#F8DCC0" font-size="12" font-weight="800">3. ЦИФРОВАЯ ПОДПИСЬ И НАБРОСКИ</text>
  <text x="65" y="508" fill="#C19E75" font-size="10">Роспись стилусом/пальцем с мгновенной вставкой в документ ПК (Ctrl+V)</text>

  <!-- Canvas -->
  <rect x="65" y="525" width="410" height="250" rx="14" fill="#1A1110" stroke="#3E2826"/>
  <!-- Vector Signature Curve -->
  <path d="M100 680 C 140 600, 160 720, 200 640 S 260 590, 310 650 S 370 690, 420 620" fill="none" stroke="#FFFFFF" stroke-width="3.5" stroke-linecap="round"/>
  <line x1="90" y1="710" x2="450" y2="710" stroke="#593F3C" stroke-width="1"/>
  <text x="270" y="740" fill="#C19E75" font-size="10" text-anchor="middle">Сглаживание Безье • Прозрачный альфа-фон</text>

  <!-- Right Palette & Actions -->
  <g transform="translate(490 525)">
    <rect x="0" y="0" width="105" height="250" rx="12" fill="#2E1E1C" stroke="#593F3C"/>
    <text x="52" y="24" fill="#F8DCC0" font-size="9" font-weight="800" text-anchor="middle">ЦВЕТ ПЕРА</text>

    <!-- Colors -->
    <circle cx="28" cy="46" r="10" fill="#FFFFFF" stroke="#F8DCC0" stroke-width="2"/>
    <circle cx="56" cy="46" r="10" fill="#38BDF8"/>
    <circle cx="84" cy="46" r="10" fill="#22C55E"/>

    <text x="52" y="80" fill="#F8DCC0" font-size="9" font-weight="800" text-anchor="middle">ТОЛЩИНА</text>
    <rect x="12" y="92" width="81" height="22" rx="4" fill="#3D2A28"/>
    <text x="52" y="107" fill="#F8DCC0" font-size="9" font-weight="700" text-anchor="middle">4px Средне</text>

    <rect x="10" y="150" width="85" height="38" rx="6" fill="#F8DCC0"/>
    <text x="52" y="174" fill="#221615" font-size="9" font-weight="800" text-anchor="middle">Вставить ПК</text>

    <rect x="10" y="196" width="85" height="34" rx="6" fill="#3D2A28" stroke="#593F3C"/>
    <text x="52" y="217" fill="#F8DCC0" font-size="9" font-weight="600" text-anchor="middle">Сохранить</text>
  </g>
</g>

<!-- PANEL 4: SCREEN CAPTURE & DESKTOP PREVIEW -->
<g clip-path="url(#c4)">
  <rect x="665" y="460" width="575" height="340" fill="#090E17"/>
  <rect x="665" y="460" width="575" height="340" stroke="#2D3C59" stroke-width="1.5" rx="20" fill="none"/>

  <text x="690" y="492" fill="#D0E2FF" font-size="12" font-weight="800">4. ЗАХВАТ И ПРЕВЬЮ ЭКРАНА WINDOWS</text>
  <text x="690" y="508" fill="#54617A" font-size="10">Снимок дисплея в реальном времени с сохранением в Pictures\\Pult</text>

  <!-- Screenshot Display Card -->
  <rect x="690" y="525" width="410" height="250" rx="14" fill="#131B2A" stroke="#2D3C59"/>
  <!-- Mini Desktop Simulation inside preview -->
  <rect x="704" y="540" width="382" height="220" rx="8" fill="#050B14"/>
  <rect x="720" y="555" width="220" height="130" rx="6" fill="#101A2B" stroke="#2D3C59"/>
  <text x="732" y="575" fill="#D0E2FF" font-size="9" class="mono">C:\\Users\\Vlad\\Pult&gt; make</text>
  <text x="732" y="595" fill="#22C55E" font-size="9" class="mono">[OK] Keep-Alive 1.04ms</text>
  <!-- Mini taskbar -->
  <rect x="704" y="736" width="382" height="24" rx="0" fill="#0E1624"/>
  <rect x="880" y="742" width="12" height="12" rx="2" fill="#38BDF8"/>

  <!-- Right Actions -->
  <g transform="translate(1115 525)">
    <rect x="0" y="0" width="105" height="250" rx="12" fill="#131B2A" stroke="#2D3C59"/>
    <text x="52" y="24" fill="#D0E2FF" font-size="9" font-weight="800" text-anchor="middle">ДЕЙСТВИЯ</text>

    <rect x="10" y="40" width="85" height="38" rx="6" fill="#D0E2FF"/>
    <text x="52" y="64" fill="#090E17" font-size="9" font-weight="800" text-anchor="middle">Снимок</text>

    <rect x="10" y="90" width="85" height="36" rx="6" fill="#1B263B" stroke="#2D3C59"/>
    <text x="52" y="112" fill="#D0E2FF" font-size="9" font-weight="700" text-anchor="middle">В буфер ПК</text>

    <rect x="10" y="136" width="85" height="36" rx="6" fill="#1B263B" stroke="#2D3C59"/>
    <text x="52" y="158" fill="#D0E2FF" font-size="9" font-weight="700" text-anchor="middle">На диск ПК</text>

    <text x="52" y="210" fill="#54617A" font-size="8" text-anchor="middle">Pictures\\Pult</text>
  </g>
</g>
</svg>`;
}

function buildThemesShowcaseSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="380" viewBox="0 0 1280 380" role="img" aria-label="Pult Theme Palettes">
<defs>
  <style>${STYLES}</style>
</defs>

<rect width="1280" height="380" fill="#080C0E"/>

<text x="640" y="38" fill="#EEF3F1" font-size="16" font-weight="800" text-anchor="middle" letter-spacing="0.8">5 ПРЕМИАЛЬНЫХ ТЕМАТИЧЕСКИХ ПАЛИТР PULT</text>
<text x="640" y="58" fill="#758585" font-size="11" font-weight="600" text-anchor="middle">ОРИГИНАЛЬНЫЕ ЦВЕТОВЫЕ СИСТЕМЫ В ЭСТЕТИКЕ APPLE × CLAUDE × OKTO</text>

<!-- 1. AURORA -->
<g transform="translate(60 85)">
  <rect x="0" y="0" width="210" height="240" rx="16" fill="#101719" stroke="#354449" stroke-width="1.5"/>
  <circle cx="105" cy="50" r="24" fill="#B4EDCC"/>
  <text x="105" y="105" fill="#B4EDCC" font-size="14" font-weight="800" text-anchor="middle">Aurora</text>
  <text x="105" y="125" fill="#71A78A" font-size="10" font-weight="600" text-anchor="middle">Изумрудный рассвет</text>
  <rect x="25" y="150" width="160" height="28" rx="6" fill="#1B2629" stroke="#354449"/>
  <text x="105" y="168" fill="#EEF3F1" font-size="10" class="mono" text-anchor="middle">#101719 • #B4EDCC</text>
  <rect x="25" y="190" width="160" height="24" rx="6" fill="#B4EDCC" fill-opacity="0.15"/>
  <text x="105" y="206" fill="#B4EDCC" font-size="10" font-weight="700" text-anchor="middle">По умолчанию</text>
</g>

<!-- 2. DUSK -->
<g transform="translate(300 85)">
  <rect x="0" y="0" width="210" height="240" rx="16" fill="#171228" stroke="#463A68" stroke-width="1.5"/>
  <circle cx="105" cy="50" r="24" fill="#E7D4FF"/>
  <text x="105" y="105" fill="#E7D4FF" font-size="14" font-weight="800" text-anchor="middle">Dusk</text>
  <text x="105" y="125" fill="#8B578F" font-size="10" font-weight="600" text-anchor="middle">Сумеречный индиго</text>
  <rect x="25" y="150" width="160" height="28" rx="6" fill="#231B38" stroke="#463A68"/>
  <text x="105" y="168" fill="#EEF3F1" font-size="10" class="mono" text-anchor="middle">#171228 • #E7D4FF</text>
  <rect x="25" y="190" width="160" height="24" rx="6" fill="#E7D4FF" fill-opacity="0.15"/>
  <text x="105" y="206" fill="#E7D4FF" font-size="10" font-weight="700" text-anchor="middle">Глубокий вечер</text>
</g>

<!-- 3. EMBER -->
<g transform="translate(540 85)">
  <rect x="0" y="0" width="210" height="240" rx="16" fill="#221615" stroke="#593F3C" stroke-width="1.5"/>
  <circle cx="105" cy="50" r="24" fill="#F8DCC0"/>
  <text x="105" y="105" fill="#F8DCC0" font-size="14" font-weight="800" text-anchor="middle">Ember</text>
  <text x="105" y="125" fill="#A45F43" font-size="10" font-weight="600" text-anchor="middle">Тёплая терракота</text>
  <rect x="25" y="150" width="160" height="28" rx="6" fill="#2E1E1C" stroke="#593F3C"/>
  <text x="105" y="168" fill="#EEF3F1" font-size="10" class="mono" text-anchor="middle">#221615 • #F8DCC0</text>
  <rect x="25" y="190" width="160" height="24" rx="6" fill="#F8DCC0" fill-opacity="0.15"/>
  <text x="105" y="206" fill="#F8DCC0" font-size="10" font-weight="700" text-anchor="middle">Уютный огонь</text>
</g>

<!-- 4. MIDNIGHT -->
<g transform="translate(780 85)">
  <rect x="0" y="0" width="210" height="240" rx="16" fill="#090E17" stroke="#2D3C59" stroke-width="1.5"/>
  <circle cx="105" cy="50" r="24" fill="#D0E2FF"/>
  <text x="105" y="105" fill="#D0E2FF" font-size="14" font-weight="800" text-anchor="middle">Midnight</text>
  <text x="105" y="125" fill="#54617A" font-size="10" font-weight="600" text-anchor="middle">Ночной ультрамарин</text>
  <rect x="25" y="150" width="160" height="28" rx="6" fill="#131B2A" stroke="#2D3C59"/>
  <text x="105" y="168" fill="#EEF3F1" font-size="10" class="mono" text-anchor="middle">#090E17 • #D0E2FF</text>
  <rect x="25" y="190" width="160" height="24" rx="6" fill="#D0E2FF" fill-opacity="0.15"/>
  <text x="105" y="206" fill="#D0E2FF" font-size="10" font-weight="700" text-anchor="middle">Космический лёд</text>
</g>

<!-- 5. OLED -->
<g transform="translate(1020 85)">
  <rect x="0" y="0" width="210" height="240" rx="16" fill="#000000" stroke="#1F2432" stroke-width="1.5"/>
  <circle cx="105" cy="50" r="24" fill="#38BDF8"/>
  <text x="105" y="105" fill="#38BDF8" font-size="14" font-weight="800" text-anchor="middle">OLED Pure</text>
  <text x="105" y="125" fill="#8B949E" font-size="10" font-weight="600" text-anchor="middle">Абсолютный чёрный</text>
  <rect x="25" y="150" width="160" height="28" rx="6" fill="#0C0E14" stroke="#1F2432"/>
  <text x="105" y="168" fill="#EEF3F1" font-size="10" class="mono" text-anchor="middle">#000000 • #38BDF8</text>
  <rect x="25" y="190" width="160" height="24" rx="6" fill="#38BDF8" fill-opacity="0.15"/>
  <text x="105" y="206" fill="#38BDF8" font-size="10" font-weight="700" text-anchor="middle">Максимум контраста</text>
</g>
</svg>`;
}

fs.writeFileSync(path.join(outDir, 'pult-hero-dock.svg'), buildHeroDockSvg());
fs.writeFileSync(path.join(outDir, 'pult-suite-showcase.svg'), buildSuiteShowcaseSvg());
fs.writeFileSync(path.join(outDir, 'pult-themes.svg'), buildThemesShowcaseSvg());

console.log('Successfully generated new visual showcase assets!');
