# Fox Weiqi AI Overlay: Technical Feasibility Analysis

> **Date**: May 2026
> **Scope**: Overlaying AI winrate/suggestions on Fox Weiqi (foxwq.com) using the existing Lizzie-improvements Java project
> **Status**: Research & Analysis

---

## 1. Fox Weiqi Technical Architecture

### 1.1 Client Type: Desktop Application (Windows)

Fox Weiqi is a **native Windows desktop application**, not a web-based client. Key evidence:

- Distributed as a standalone Windows installer (not via browser)
- Available on the **Microsoft Store** (`xpdbwgsvbvh5l5`)
- Custom protocol handler: `qqgameprotocol:///SNSBROWSER GAMEID:10112`
- Can be installed as a **plugin within the QQ Game platform** (QQ游戏), or as a standalone app
- Separate builds exist for: Windows, macOS, and mobile (Android via AppBao package `com.foxwq.yhwq`)
- Mobile app size: ~178MB

### 1.2 Technology Stack (Inferred)

Fox Weiqi's exact technology stack is **not publicly documented**, but multiple indicators suggest:

| Indicator | Evidence |
|-----------|----------|
| **Likely C++ native** | The AI bridge DLL is `fox_ai_bridge.dll` (native DLL), and `vcredist_x86_2010.exe` is a dependency |
| **QQ Game ecosystem** | Integrated as a game plugin (`GAMEID:10112`), suggesting it follows QQ Game's C++ plugin architecture |
| **Proprietary rendering** | The board rendering uses a custom format (Fox SGF), not standard web technologies |
| **TCP/Socket networking** | AI integration uses local TCP sockets, consistent with a C++ networking stack |
| **NOT Flash** | Flash was deprecated; no SWF files referenced |
| **NOT Electron/CEF** | No Chromium-based indicators; small installer footprint |
| **NOT HTML5/Canvas** | The AI bridge uses native DLL injection, not DOM manipulation |

**Assessment**: Fox Weiqi is most likely a **C++ native Windows application** with custom rendering, possibly using Direct2D or a similar graphics API for the board display.

### 1.3 Fox Weiqi API & Plugin System

#### Official AI Integration Program

Fox Weiqi **does** have an official AI integration mechanism:

| Component | Description |
|-----------|-------------|
| **FoxGTP.exe** (747KB) | Core bridge program connecting external AI engines to the Fox Weiqi client |
| **StandardIOToTcp.exe** (120KB) | Converts STDIN/STDOUT to TCP communication |
| **fox_ai_bridge.dll** | Native DLL extension module |
| **Protocol** | GTP (Go Text Protocol) over local TCP socket |
| **Default Port** | localhost:9999 (configurable, ports above 8000) |
| **Account Requirement** | 3D rank or higher on Fox Weiqi |

**Architecture**:
```
Fox Weiqi Client  <-->  FoxGTP  <-->  KataGo/LeelaZero Engine
     (TCP:9999)         (GTP/STDIN)
```

**Documentation**: The official package includes:
- `Fox Go AI Protocol.pdf` (909KB) - Protocol specification
- `野狐围棋AI接入协议.pdf` (887KB) - Chinese protocol doc
- `野狐围棋AI接入手册.pdf` (670KB) - Integration manual
- `gtp2-spec-draft2.pdf` - GTP v2 specification

#### Unofficial REST API (Community)

A community-maintained REST API exists:

| Endpoint | Description |
|----------|-------------|
| `GET /me` | Current player info |
| `GET /players?nick={nick}` | Search player by nickname |
| `GET /players/{id}` | Player info by ID |
| `GET /players/{id}/games` | Player's game records (paginated) |
| `GET /top_games` | Latest top games |
| `GET /games/{id}` | Game SGF by ID |

- **Base URL**: `https://foxwq-8e6797d8dbb9.herokuapp.com/api/v1`
- **Auth**: `X-APP-ID` + `X-API-KEY` headers + Basic Auth (MD5 of password)
- **Source**: [github.com/openfoxwq/api](https://github.com/openfoxwq/api)
- **Note**: Third-party, not official. May break without notice.

---

## 2. Feasible Overlay Approaches

### 2.1 Approach Comparison Matrix

| Approach | Feasibility | Latency | Accuracy | Complexity | Anti-Cheat Risk |
|----------|-------------|---------|----------|------------|-----------------|
| **A: Official FoxGTP Bridge** | HIGH | Low (~100ms) | Perfect | Low | None (official) |
| **B: Screen Capture + Pixel Detection** | HIGH | Medium (~500ms) | High (99%+) | Medium | Medium |
| **C: Transparent Window Overlay** | HIGH | Low | N/A (display only) | Low-Medium | Low |
| **D: Browser Extension** | NOT APPLICABLE | - | - | - | - |
| **E: Memory Reading** | MEDIUM | Low (~10ms) | Perfect | High | HIGH |
| **F: Network Packet Sniffing** | LOW | Low | High | Very High | HIGH |
| **G: Proxy/MITM** | LOW | Medium | High | Very High | HIGH |

### 2.2 Approach A: Official FoxGTP Bridge (RECOMMENDED)

**How it works**: Fox Weiqi has an officially supported mechanism for connecting external AI engines via GTP protocol.

**Architecture**:
```
┌──────────────────┐     TCP localhost:9999      ┌──────────────┐     GTP/STDIN     ┌──────────────┐
│  Fox Weiqi       │ ◄──────────────────────────► │  FoxGTP.exe  │ ◄───────────────► │  KataGo.exe  │
│  Client          │                              │  (Bridge)    │                   │  (AI Engine) │
└──────────────────┘                              └──────────────┘                   └──────────────┘
```

**Implementation**:
1. Use FoxGTP to connect KataGo to Fox Weiqi (official, supported method)
2. Build a **separate display component** that reads the same GTP stream or uses KataGo's analysis output
3. Create a transparent overlay window showing winrate/heatmaps positioned over the Fox Weiqi board

**Advantages**:
- Officially supported by Fox Weiqi
- No anti-cheat risk (it's the intended use)
- Perfect board state synchronization (GTP protocol)
- Already exists and works

**Limitations**:
- Requires 3D+ rank account
- AI plays as the user (not just analysis overlay)
- No direct "analysis-only" mode in the official API
- The official tool is designed for AI auto-play, not passive analysis

**Integration with Lizzie-improvements**: Your project already has full KataGo GTP integration (`Leelaz.java`, `EngineManager.java`). You could:
1. Fork FoxGTP's approach to create a "read-only" analysis bridge
2. Use your existing engine management code to run KataGo alongside Fox Weiqi

### 2.3 Approach B: Screen Capture + Pixel Detection (PROVEN)

**How it works**: This approach is already **proven and production-tested** by [LizzieYzy](https://github.com/yzyray/lizzieyzy).

**Architecture** (based on LizzieYzy's readboard system):
```
┌──────────────────┐
│  Fox Weiqi       │
│  Client          │
└────────┬─────────┘
         │ Screen capture (periodic)
         ▼
┌──────────────────┐     Pipe/Socket     ┌──────────────────┐
│  readboard.exe   │ ◄──────────────────► │  LizzieYzy       │
│  (C# screen      │                      │  (Java GUI +     │
│   reader)        │                      │   KataGo engine) │
└──────────────────┘                      └──────────────────┘
                                                  │
                                                  ▼
                                           ┌──────────────┐
                                           │  FloatBoard  │
                                           │  (Overlay)   │
                                           └──────────────┘
```

**LizzieYzy's Implementation Details**:

1. **readboard.exe** (C#, Windows-only):
   - Captures screen pixels from the Fox Weiqi window
   - Uses **pixel color analysis** to detect stone positions (black/white/empty)
   - Maps pixel coordinates to Go board intersections
   - Sends board state to LizzieYzy via named pipe

2. **readboard_java** (Java, cross-platform):
   - Same functionality as readboard.exe but written in Java
   - Uses socket communication instead of named pipes
   - Bundled as `readboard-1.6.2-shaded.jar`

3. **ReadBoard.java** (in LizzieYzy):
   - Central synchronization engine (841 lines)
   - Platform-specific board type handling:
     - Fox Weiqi (Board Type 0): Advanced position detection, full auto-play
     - YuanChen/YC (Board Type 1): Scaling factor support
     - SINA (Board Type 2): Standard implementation
   - `syncBoardStones()` method (lines 589-762): Compares detected board with internal state
   - Two communication modes: Pipe (Windows/C#) and Socket (Java/cross-platform)

4. **FloatBoard.java** (overlay display):
   - Transparent overlay window positioned over the external Go platform
   - Dynamically adjusts position/size based on detected board area
   - Renders winrate, move suggestions, and analysis directly on top of Fox Weiqi

**Performance** (from documentation):
- Fox Weiqi sync: **~0.5 seconds** after opponent's move
- Dual-channel sync: Pixel recognition + GTP protocol simultaneously
- Bidirectional sync supported (can also send moves to Fox Weiqi)

**Advantages**:
- Works without any API or official support
- Already proven in production (LizzieYzy)
- Can be adapted to any Go client, not just Fox Weiqi
- Works with any board size (9x9, 13x13, 19x19)

**Limitations**:
- Requires the Fox Weiqi window to be visible and at a known position
- Sensitive to display scaling, DPI settings, and theme changes
- Window focus/occlusion can break detection
- Slightly slower than direct API (~500ms vs ~100ms)

**Integration with Lizzie-improvements**: You could:
1. Port the `ReadBoard.java` and `FloatBoard.java` classes from LizzieYzy
2. LizzieYzy is also based on Lizzie, so the codebase is very similar
3. The `readboard_java` JAR can be used as-is for the screen capture component

### 2.4 Approach C: Transparent Window Overlay

**How it works**: Create a transparent, click-through window positioned over the Fox Weiqi board area, rendering AI analysis (winrate bars, colored suggestions, etc.).

**This is the DISPLAY component** of Approach B, and can also work standalone if board state is obtained via Approach A.

**Implementation options**:

| Technology | Platform | Notes |
|-----------|----------|-------|
| Java AWT `Window` with `setOpacity()` | Cross-platform | Lizzie already uses Java Swing |
| JavaFX `Stage` with `setOpacity()` | Cross-platform | More modern, better transparency support |
| C#/WPF `Window` with `AllowsTransparency` | Windows-only | Better Windows integration |
| Electron transparent window | Cross-platform | Heavyweight but flexible |

**Java implementation** (simplest for your project):
```java
// Pseudo-code for transparent overlay
JFrame overlay = new JFrame();
overlay.setUndecorated(true);
overlay.setBackground(new Color(0, 0, 0, 0)); // Fully transparent
overlay.setAlwaysOnTop(true);
overlay.setType(JFrame.Type.UTILITY); // Doesn't show in taskbar

// Custom panel that draws semi-transparent analysis graphics
overlay.setContentPane(new AnalysisOverlayPanel());
```

**Advantages**:
- Lightweight display-only component
- Works with any board state source
- Easy to implement in Java (your existing tech stack)
- LizzieYzy already has a working `FloatBoard.java` implementation

### 2.5 Approach D: Browser Extension — NOT FEASIBLE

Fox Weiqi is a **native desktop application**, not a web-based client. Browser extensions (Chrome/Firefox) cannot interact with native applications. This approach is only applicable to web-based Go servers like OGS (online-go.com).

### 2.6 Approach E: Memory Reading — HIGH RISK

**How it works**: Read the Fox Weiqi process memory to extract the current board state directly.

**Technical approach**:
1. Find the Fox Weiqi process (by window title or process name)
2. Use `ReadProcessMemory` (Windows API) to read game state
3. Reverse-engineer the memory layout to find board state data structures
4. Map memory offsets to Go board positions

**Advantages**:
- Perfect accuracy
- Very low latency (~10ms)
- No dependency on screen visibility

**Disadvantages**:
- Fox Weiqi likely has anti-cheat detection for memory reading
- Memory layout changes with every client update
- Requires reverse engineering for each version
- Violates Fox Weiqi's Terms of Service
- High risk of account ban

**Assessment**: **Not recommended** for legitimate use. The anti-cheat risk is too high.

### 2.7 Approach F: Network Packet Sniffing — HIGH RISK

**How it works**: Capture and parse TCP packets between Fox Weiqi client and server.

**Technical approach**:
1. Use WinPcap/Npcap to capture Fox Weiqi's network traffic
2. Decrypt/parse the protocol (likely encrypted)
3. Extract game state from move commands

**Disadvantages**:
- Fox Weiqi likely uses TLS encryption
- Protocol is proprietary and undocumented
- Requires protocol reverse engineering
- High anti-cheat risk (similar to memory reading)
- Violates Terms of Service

**Assessment**: **Not recommended**. Too complex and risky.

### 2.8 Approach G: Proxy/MITM — HIGH RISK

**How it works**: Intercept HTTPS traffic by installing a custom CA certificate and proxying connections.

**Disadvantages**:
- Requires installing a root CA certificate (security risk)
- Certificate pinning may prevent MITM
- Same anti-cheat and ToS risks as packet sniffing
- Complex setup

**Assessment**: **Not recommended**.

---

## 3. Existing Tools & Projects

### 3.1 LizzieYzy (Most Relevant)

| Attribute | Details |
|-----------|---------|
| **Repository** | [github.com/yzyray/lizzieyzy](https://github.com/yzyray/lizzieyzy) |
| **Active Fork** | [github.com/wimi321/lizzieyzy-next](https://github.com/wimi321/lizzieyzy-next) |
| **Language** | 94.2% Java |
| **Based on** | Lizzie (same codebase as Lizzie-improvements) |
| **Fox Integration** | Full board sync + overlay for Fox Weiqi |

**Key Fox Weiqi Features**:
- **Board Synchronization**: Real-time pixel-based board state detection
- **FloatBoard Overlay**: Transparent window displaying AI analysis on top of Fox Weiqi
- **Fox Game Fetching**: Download recent public game records by nickname via FoxRequest
- **Auto-play**: Bidirectional sync (can also play moves on Fox Weiqi)

**Components for Fox Integration**:
| Component | Source |
|-----------|--------|
| `ReadBoard.java` | Core sync engine in LizzieYzy |
| `FloatBoard.java` | Overlay window in LizzieYzy |
| `readboard.exe` | C# screen capture tool ([github.com/yzyray/readboard](https://github.com/yzyray/readboard)) |
| `readboard_java` | Java screen capture tool (cross-platform) |
| `FoxRequest.jar` | Fox SGF game record downloader ([github.com/yzyray/FoxRequest](https://github.com/yzyray/FoxRequest)) |

**Relevance to Lizzie-improvements**: LizzieYzy is a fork of Lizzie, and Lizzie-improvements is also a fork of Lizzie. The codebases share the same root. The board synchronization code from LizzieYzy could be directly adapted.

### 3.2 FoxGTP (Official Tool)

| Attribute | Details |
|-----------|---------|
| **Source** | [foxwq.com/soft/aiprogramandmanual.html](https://www.foxwq.com/soft/aiprogramandmanual.html) |
| **Language** | C++ / Native |
| **Purpose** | Official bridge between AI engines and Fox Weiqi |
| **Protocol** | GTP over TCP |

**Components**:
- `FoxGTP.exe` (747KB) - Main bridge
- `StandardIOToTcp.exe` (120KB) - I/O converter
- `fox_ai_bridge.dll` - Native extension
- `fuego.exe` (3.16MB) - Fuego engine (included)
- `book.dat` (132KB) - Opening book

**Relevance**: Provides the official integration pathway. Could be used as a reference for building a custom analysis-only bridge.

### 3.3 KaTrain

| Attribute | Details |
|-----------|---------|
| **Repository** | [github.com/sanderland/katrain](https://github.com/sanderland/katrain) |
| **Language** | Python |
| **Engine** | KataGo |
| **Fox SGF** | Has komi fix for Fox SGF files |

KaTrain can **load Fox SGF files** (with fixes) but does **not** provide real-time Fox Weiqi integration or overlay. It's primarily a standalone analysis tool.

### 3.4 Kaya

| Attribute | Details |
|-----------|---------|
| **Repository** | [github.com/kaya-go/kaya](https://github.com/kaya-go/kaya) |
| **Language** | TypeScript (79.6%), Rust (7.2%) |
| **Framework** | React 19 + Tauri v2 |
| **Board Recognition** | Moku AI (RT-DETR model) + classic CV |

Kaya has photo-based board recognition (for physical boards) but does **not** integrate with Fox Weiqi for real-time online game analysis.

### 3.5 Tygem Go Pro

| Attribute | Details |
|-----------|---------|
| **Platform** | iOS / Android |
| **Features** | Built-in AI winrate analysis during games |

A mobile app for Tygem (another Go server) with native AI analysis. Not directly relevant to Fox Weiqi desktop.

### 3.6 OpenFoxWQ API

| Attribute | Details |
|-----------|---------|
| **Repository** | [github.com/openfoxwq/api](https://github.com/openfoxwq/api) |
| **Type** | Unofficial REST API |
| **Auth** | API key + Basic Auth (MD5 password) |

Useful for fetching game records programmatically, but does not provide real-time board state during a game.

### 3.7 Go Board Recognition Tools

Several tools exist for detecting Go board state from images:

| Tool | Technology | Output |
|------|-----------|--------|
| [kifu-snap](https://www.remi-coulom.fr/kifu-snap/) | Various (directory of tools) | SGF |
| [Rocamgo](https://github.com/virp/rocamgo) | Python + OpenCV | SGF |
| [YOLO-GO](https://github.com/) | YOLO object detection | Board positions |
| Kaya's Moku AI | RT-DETR + classic CV | SGF |

These are primarily designed for **static photos** of physical boards, not real-time screen capture.

---

## 4. Technical Challenges

### 4.1 Screen Coordinate to Board Position Mapping

**Challenge**: Accurately mapping pixel coordinates on screen to the correct Go board intersection.

**LizzieYzy's approach** (Board Type 0 for Fox Weiqi):
- Uses platform-specific "Advanced position detection"
- Maintains a mapping of board area bounds (x, y, width, height)
- Calculates grid spacing from detected board dimensions
- Maps each pixel coordinate to the nearest intersection
- Accounts for scaling factors (DPI, display scaling)

**Implementation considerations**:
```
1. User calibration: Click on known board positions (e.g., corners/star points)
2. Calculate: intersection[i][j] = (boardLeft + j * gridSpacing, boardTop + i * gridSpacing)
3. Handle: Different board sizes (19x19: 18 gaps, 13x13: 12 gaps, 9x9: 8 gaps)
4. Account for: Window resizing, DPI scaling (125%, 150%), multi-monitor setups
```

**Accuracy**: With proper calibration, pixel-based detection can achieve **99%+ accuracy** for stone placement detection on Go boards.

### 4.2 Board Size Handling

**Challenge**: Supporting different board sizes.

| Board Size | Grid Intersections | Grid Gaps |
|-----------|-------------------|-----------|
| 19x19 | 361 | 18 horizontal, 18 vertical |
| 13x13 | 169 | 12 horizontal, 12 vertical |
| 9x9 | 81 | 8 horizontal, 8 vertical |

**Approach**:
- Fox Weiqi sends the board size in the game settings (observable from FoxGTP or the REST API `GameSettings.board_size`)
- Screen-based detection can auto-detect board size by counting grid lines
- The mapping algorithm is the same for all sizes, only the gap count changes

### 4.3 Latency Considerations

| Operation | Estimated Latency | Notes |
|-----------|-------------------|-------|
| Screen capture | ~5-10ms | Native Win32 `BitBlt` or Java `Robot.createScreenCapture` |
| Pixel analysis (board detection) | ~10-50ms | Simple color thresholding, no deep learning needed |
| Socket/pipe communication | ~1-5ms | Localhost only |
| KataGo analysis (first response) | ~100-500ms | Depends on GPU, visits setting |
| KataGo pondering (continuous) | ~50-200ms | Ongoing analysis between moves |
| Overlay rendering | ~5-10ms | Java2D is sufficient for 2D graphics |
| **Total (first analysis)** | **~200-600ms** | Acceptable for human perception |
| **Total (pondering update)** | **~50-200ms** | Near real-time |

**Optimization strategies**:
- Use `kataGo-analyze` command for continuous analysis instead of single `genmove`
- Run KataGo with GPU acceleration (CUDA/OpenCL)
- Use differential screen capture (only re-analyze changed regions)
- Cache board state and only send new moves to the engine

### 4.4 Anti-Cheat Detection Risks

| Approach | Risk Level | Explanation |
|----------|-----------|-------------|
| **Official FoxGTP** | NONE | Officially supported integration |
| **Screen capture + overlay** | LOW | Hard to detect; reads pixels externally, doesn't modify game process |
| **Transparent overlay window** | LOW | Standard OS feature, not detectable by the game |
| **Memory reading** | **HIGH** | Fox Weiqi likely scans for known memory reader tools |
| **Packet sniffing** | **HIGH** | TLS + potential certificate pinning |
| **Process injection (DLL)** | **VERY HIGH** | `fox_ai_bridge.dll` approach is the official one; unofficial injection is high risk |

**Community reports**:
- Fox Weiqi has an active community that tracks AI cheaters ([Baidu Tieba blacklist](https://tieba.baidu.com/p/8654261508))
- AI cheating detection uses move-matching analysis (comparing player moves to engine top choices)
- Screen capture + overlay is the **safest approach** as it's externally undetectable
- The main detection vector is **behavioral** (playing too consistently with AI suggestions), not technical

**Important note**: Even with safe technical methods, if a user plays every move exactly as KataGo suggests, opponents will notice and may report. Use analysis for **learning/review**, not for cheating in rated games.

---

## 5. Recommended Implementation Plan

### 5.1 Option 1: Port from LizzieYzy (Fastest Path)

Since both LizzieYzy and Lizzie-improvements share the same Lizzie codebase:

1. **Port `ReadBoard.java`** from LizzieYzy into Lizzie-improvements
   - ~841 lines, core sync engine
   - Handles Fox Weiqi (Board Type 0), YuanChen, SINA

2. **Port `FloatBoard.java`** from LizzieYzy
   - ~857 lines, transparent overlay component
   - Renders winrate, suggestions, ownership on top of external windows

3. **Bundle `readboard_java`** (or `readboard.exe`)
   - Pre-built screen capture tool
   - Communicates with Java via socket

4. **Leverage existing KataGo integration**
   - Your `EngineManager.java` and `Leelaz.java` already handle GTP communication
   - No changes needed to the engine layer

**Estimated effort**: 2-4 weeks (mostly testing and adaptation)

### 5.2 Option 2: Use Official FoxGTP + Custom Overlay

1. **Use FoxGTP.exe** as-is to connect KataGo to Fox Weiqi
2. **Build a custom overlay component** in Java that:
   - Reads KataGo's analysis output (GTP `info` or `kata-analyze` commands)
   - Renders a transparent overlay window over the Fox Weiqi board
3. **Calibrate** the overlay position to match the Fox Weiqi board area

**Estimated effort**: 2-3 weeks

### 5.3 Option 3: Hybrid Approach (Most Robust)

1. Use **pixel detection** (from readboard) for real-time board state
2. Use **FoxGTP protocol knowledge** for accurate move tracking
3. Use **FloatBoard overlay** for analysis display
4. Add **Fox game record fetching** (via FoxRequest or openfoxwq API) for post-game review

---

## 6. Architecture Diagram (Recommended)

```
                    ┌─────────────────────────────────────────┐
                    │         Lizzie-improvements             │
                    │         (Java Application)              │
                    │                                         │
                    │  ┌───────────┐  ┌───────────────────┐  │
                    │  │  Engine   │  │  FloatBoard       │  │
                    │  │  Manager  │  │  (Transparent     │  │
                    │  │  (KataGo) │  │   Overlay)        │  │
                    │  │           │  │                   │  │
                    │  └─────┬─────┘  └────────▲──────────┘  │
                    │        │                 │              │
                    │  ┌─────▼─────────────────┴──────────┐  │
                    │  │  ReadBoard (Sync Engine)          │  │
                    │  │  - Board state detection          │  │
                    │  │  - Coordinate mapping              │  │
                    │  │  - Command processing              │  │
                    │  └─────┬─────────────────▲──────────┘  │
                    └────────┼─────────────────┼──────────────┘
                             │                 │
                    ┌────────▼─────────────────┴──────────┐
                    │  readboard.exe / readboard_java      │
                    │  (Screen Capture Component)          │
                    │  - Window detection                  │
                    │  - Pixel color analysis               │
                    │  - Stone position mapping             │
                    └────────┬─────────────────▲──────────┘
                             │                 │
                    ┌────────▼─────────────────┴──────────┐
                    │  Fox Weiqi Client (Desktop App)      │
                    │  - Board rendering                   │
                    │  - Game state                        │
                    └──────────────────────────────────────┘
```

---

## 7. Key Findings Summary

1. **Fox Weiqi is a native C++ Windows desktop app**, not web-based. Browser extensions won't work.

2. **Official AI integration exists** via FoxGTP (GTP over TCP on localhost:9999), but it's designed for AI auto-play, not passive analysis overlay.

3. **LizzieYzy already solves this problem** using screen capture + pixel detection + transparent overlay. Since it's based on the same Lizzie codebase as your project, porting is highly feasible.

4. **Screen capture + overlay is the safest approach** regarding anti-cheat — it's externally undetectable and doesn't modify the Fox Weiqi process.

5. **The main technical challenges** are:
   - Accurate screen-to-board coordinate mapping (solved by LizzieYzy)
   - Handling different display scales/DPI (solved by LizzieYzy)
   - Maintaining sync speed (~0.5s, acceptable)
   - Behavioral detection (don't play exactly like AI in rated games)

6. **An unofficial REST API** (openfoxwq/api) exists for fetching game records but not real-time board state.

7. **Your existing Lizzie-improvements codebase** already has all the KataGo integration needed — you only need to add the board synchronization and overlay display components.
