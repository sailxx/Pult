# Pult Network Protocol Specification

Pult uses two network protocols for local communication:
- **UDP Port 47801**: Local Host Discovery
- **TCP Port 47800**: HTTP REST API for Remote Control

---

## 1. Discovery Protocol (UDP 47801)

The mobile client broadcasts discovery packets to find all active Pult hosts in the Wi-Fi network.

### Request (Phone -> Broadcast):
- **Packet content (ASCII)**: `PULT?`
- **Destination**: Subnet broadcast address (e.g. `192.168.1.255`) and `255.255.255.255:47801`.

### Response (PC Host -> Phone):
- **Packet content (UTF-8)**: `PULT!<port>|<hostname>`
- **Example**: `PULT!47800|DESKTOP-PC`

> Note: If client-to-client UDP broadcast is blocked by the Wi-Fi router, Pult automatically performs a parallel subnet sweep using `GET /ping` across the `/24` network mask.

---

## 2. HTTP API (TCP 47800)

All endpoints (except `/ping`) require the `X-Pin` HTTP header or `?pin=` query parameter.

### 2.1 Health Check (Unauthenticated)
```http
GET /ping HTTP/1.1
```
**Response (200 OK)**:
```json
{
  "app": "pult",
  "name": "DESKTOP-PC",
  "port": 47800,
  "version": "0.2",
  "volume": 65,
  "muted": false
}
```

---

### 2.2 Applications
#### List Installed Apps
```http
GET /apps HTTP/1.1
X-Pin: 123456
```
**Response (200 OK)**:
```json
[
  {"id": "a1b2c3d4e5", "name": "Google Chrome"},
  {"id": "f6g7h8i9j0", "name": "Spotify"}
]
```

#### Fetch App Icon
```http
GET /icon?id=a1b2c3d4e5 HTTP/1.1
X-Pin: 123456
```
**Response (200 OK)**: `image/png` (96x96 px)

#### Launch Application
```http
POST /launch?id=a1b2c3d4e5 HTTP/1.1
X-Pin: 123456
```
**Response (200 OK)**:
```json
{"ok": true}
```

---

### 2.3 Volume & Audio (Core Audio API)
#### Get Status
```http
GET /volume HTTP/1.1
X-Pin: 123456
```
**Response (200 OK)**:
```json
{"volume": 65, "muted": false, "ok": true}
```

#### Set Volume Level
```http
POST /volume?set=50 HTTP/1.1
X-Pin: 123456
```

#### Toggle Mute
```http
POST /volume?mute=toggle HTTP/1.1
X-Pin: 123456
```

---

### 2.4 Media & Special Keys
```http
POST /key?name=playpause HTTP/1.1
X-Pin: 123456
```
Supported names:
`playpause`, `next`, `prev`, `stop`, `volup`, `voldown`, `mute`, `enter`, `esc`, `space`, `backspace`, `tab`, `up`, `down`, `left`, `right`.

---

### 2.5 Trackpad & Mouse
```http
POST /mouse?dx=12&dy=-8 HTTP/1.1
X-Pin: 123456
```
- Relative motion: `?dx=<int>&dy=<int>`
- Click button: `?click=left|right|middle|double`
- Button state: `?down=left` / `?up=left` (for dragging)
- Scroll wheel: `?scroll=120` (up) / `?scroll=-120` (down)

---

### 2.6 System & Power
```http
POST /system?action=lock HTTP/1.1
X-Pin: 123456
```
Supported actions:
- `lock`: Lock workstation (`Win + L`)
- `desktop`: Minimize all / show desktop (`Win + D`)
- `alttab`: Switch between windows (`Alt + Tab`)
- `monitors_off`: Power down displays (`SC_MONITORPOWER 2`)
- `sleep`: Sleep mode (`SetSuspendState`)
- `shutdown`: Initiate 30-sec shutdown
- `restart`: Initiate 30-sec restart
- `cancel_shutdown`: Abort scheduled shutdown (`shutdown /a`)

---

### 2.7 Windows & Tasks
#### List Open Windows
```http
GET /tasks HTTP/1.1
X-Pin: 123456
```
**Response (200 OK)**:
```json
[
  {"id": 65542, "title": "README.md - VS Code", "proc": "Code"},
  {"id": 131084, "title": "GitHub", "proc": "chrome"}
]
```

#### Switch to Window
```http
POST /tasks/switch?id=65542 HTTP/1.1
X-Pin: 123456
```

#### Close Window
```http
POST /tasks/close?id=65542 HTTP/1.1
X-Pin: 123456
```
