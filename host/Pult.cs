using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Windows.Forms;

[assembly: System.Reflection.AssemblyVersion("0.2.0.0")]

static class Program
{
    public const int HttpPort = 47800;
    public const int UdpPort = 47801;

    static NotifyIcon tray;
    internal static string Pin;
    internal static string LastClientIp;
    internal static DateTime LastActiveTime;
    internal static readonly string DataDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Pult");

    internal static void OnClientActivity(TcpClient c)
    {
        try
        {
            var ip = ((IPEndPoint)c.Client.RemoteEndPoint).Address.ToString();
            bool wasDisconnected = LastClientIp == null || (DateTime.Now - LastActiveTime).TotalSeconds > 20;
            LastClientIp = ip;
            LastActiveTime = DateTime.Now;
            if (wasDisconnected)
            {
                Notify("Смартфон подключен: " + ip);
            }
        }
        catch { }
    }

    [STAThread]
    static void Main(string[] args)
    {
        if (args.Length > 0 && args[0] == "--firewall") { Firewall.Add(); return; }

        bool created;
        using (var mutex = new Mutex(true, "Pult.Host.Single", out created))
        {
            if (!created) return;
            Directory.CreateDirectory(DataDir);
            Pin = LoadPin();

            Application.EnableVisualStyles();
            Server.Start();
            Discovery.Start();

            tray = new NotifyIcon();
            try { tray.Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath); }
            catch { tray.Icon = SystemIcons.Application; }
            tray.Text = "Pult";
            tray.Visible = true;

            var menu = new ContextMenuStrip();
            menu.Opening += delegate
            {
                menu.Items.Clear();
                bool isConnected = LastClientIp != null && (DateTime.Now - LastActiveTime).TotalSeconds < 20;
                var statusText = isConnected ? "● Подключен: " + LastClientIp : "○ Ожидание подключения...";
                var statusItem = menu.Items.Add(statusText);
                statusItem.Enabled = false;

                menu.Items.Add(new ToolStripSeparator());

                var pinItem = menu.Items.Add("PIN: " + Pin + " (скопировать)");
                pinItem.Click += delegate
                {
                    Clipboard.SetText(Pin);
                    Notify("PIN скопирован в буфер обмена: " + Pin);
                };

                var firstIp = LocalIps().FirstOrDefault();
                if (firstIp != null)
                {
                    var ipItem = menu.Items.Add("IP: " + firstIp + ":" + HttpPort + " (скопировать)");
                    ipItem.Click += delegate
                    {
                        Clipboard.SetText(firstIp);
                        Notify("IP скопирован в буфер обмена: " + firstIp);
                    };
                }

                menu.Items.Add(new ToolStripSeparator());
                menu.Items.Add("Все адреса и PIN", null, delegate { ShowInfo(); });
                menu.Items.Add("Разрешить в брандмауэре", null, delegate { Firewall.RunElevated(); });
                menu.Items.Add("Новый PIN", null, delegate { Pin = NewPin(); ShowInfo(); });
                menu.Items.Add(new ToolStripSeparator());
                menu.Items.Add("Выход", null, delegate { tray.Visible = false; Environment.Exit(0); });
            };

            tray.ContextMenuStrip = menu;
            tray.MouseClick += delegate(object s, MouseEventArgs e) {
                if (e.Button == MouseButtons.Left) ShowCompanionFlyout();
            };

            if (!File.Exists(Path.Combine(DataDir, "seen"))) { File.WriteAllText(Path.Combine(DataDir, "seen"), "1"); ShowCompanionFlyout(); }
            Application.Run();
        }
    }

    internal static void ShowCompanionFlyout()
    {
        try
        {
            var f = new CompanionFlyout();
            f.Show();
            f.Activate();
        }
        catch { ShowInfo(); }
    }

    internal static void Notify(string text)
    {
        if (tray != null) tray.ShowBalloonTip(4000, "Pult", text, ToolTipIcon.None);
    }

    static void ShowInfo()
    {
        var sb = new StringBuilder();
        sb.AppendLine("PIN: " + Pin);
        sb.AppendLine("Порт: " + HttpPort);
        sb.AppendLine();
        foreach (var ip in LocalIps()) sb.AppendLine(ip + ":" + HttpPort);
        sb.AppendLine();
        sb.AppendLine("Если телефон не находит ПК: меню в трее > «Разрешить в брандмауэре».");
        MessageBox.Show(sb.ToString(), "Pult", MessageBoxButtons.OK, MessageBoxIcon.None);
    }

    internal static IEnumerable<string> LocalIps()
    {
        foreach (var ni in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (ni.OperationalStatus != OperationalStatus.Up) continue;
            if (ni.NetworkInterfaceType == NetworkInterfaceType.Loopback) continue;
            foreach (var ua in ni.GetIPProperties().UnicastAddresses)
                if (ua.Address.AddressFamily == AddressFamily.InterNetwork) yield return ua.Address.ToString();
        }
    }

    static string LoadPin()
    {
        var f = Path.Combine(DataDir, "pin.txt");
        try { var p = File.ReadAllText(f).Trim(); if (p.Length == 6) return p; } catch { }
        return NewPin();
    }

    internal static string NewPin()
    {
        var b = new byte[4];
        using (var r = RandomNumberGenerator.Create()) r.GetBytes(b);
        var pin = (BitConverter.ToUInt32(b, 0) % 1000000).ToString("D6");
        File.WriteAllText(Path.Combine(DataDir, "pin.txt"), pin);
        return pin;
    }
}

static class Firewall
{
    const string Rule = "Pult";

    public static void Add()
    {
        Run("advfirewall firewall delete rule name=\"" + Rule + "\"");
        Run("advfirewall firewall add rule name=\"" + Rule + "\" dir=in action=allow protocol=TCP localport=" + Program.HttpPort + " profile=any");
        Run("advfirewall firewall add rule name=\"" + Rule + " discovery\" dir=in action=allow protocol=UDP localport=" + Program.UdpPort + " profile=any");
    }

    public static void RunElevated()
    {
        try
        {
            var psi = new ProcessStartInfo(Application.ExecutablePath, "--firewall");
            psi.Verb = "runas";
            psi.UseShellExecute = true;
            var p = Process.Start(psi);
            ThreadPool.QueueUserWorkItem(delegate
            {
                p.WaitForExit();
                Program.Notify(p.ExitCode == 0 ? "Порты 47800 и 47801 открыты в брандмауэре." : "Не удалось добавить правила.");
            });
        }
        catch { Program.Notify("Запрос прав администратора отменён."); }
    }

    static void Run(string args)
    {
        var psi = new ProcessStartInfo("netsh", args);
        psi.CreateNoWindow = true;
        psi.UseShellExecute = false;
        using (var p = Process.Start(psi)) p.WaitForExit();
    }
}

class AppEntry { public string Id, Name, Path; }

static class Apps
{
    static List<AppEntry> cache;
    static DateTime loaded;
    static readonly Dictionary<string, byte[]> icons = new Dictionary<string, byte[]>();

    public static List<AppEntry> List()
    {
        lock (icons)
        {
            if (cache != null && (DateTime.UtcNow - loaded).TotalSeconds < 60) return cache;
            var roots = new[] {
                Environment.GetFolderPath(Environment.SpecialFolder.CommonStartMenu),
                Environment.GetFolderPath(Environment.SpecialFolder.StartMenu),
                Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory),
                Environment.GetFolderPath(Environment.SpecialFolder.CommonDesktopDirectory) };
            var byName = new Dictionary<string, AppEntry>(StringComparer.OrdinalIgnoreCase);
            foreach (var root in roots)
            {
                if (string.IsNullOrEmpty(root) || !Directory.Exists(root)) continue;
                string[] files;
                try { files = Directory.GetFiles(root, "*.lnk", SearchOption.AllDirectories); } catch { continue; }
                foreach (var f in files)
                {
                    var name = System.IO.Path.GetFileNameWithoutExtension(f);
                    var low = name.ToLowerInvariant();
                    if (low.Contains("uninstall") || low.Contains("удалить") || low.Contains("справк") || low.Contains("readme")) continue;
                    if (!byName.ContainsKey(name)) byName[name] = new AppEntry { Name = name, Path = f, Id = Hash(f) };
                }
            }
            cache = byName.Values.OrderBy(a => a.Name, StringComparer.CurrentCultureIgnoreCase).ToList();
            loaded = DateTime.UtcNow;
            return cache;
        }
    }

    static string Hash(string s)
    {
        using (var md5 = MD5.Create())
            return BitConverter.ToString(md5.ComputeHash(Encoding.UTF8.GetBytes(s.ToLowerInvariant()))).Replace("-", "").Substring(0, 10).ToLowerInvariant();
    }

    public static AppEntry Find(string id) { return List().FirstOrDefault(a => a.Id == id); }

    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    static extern uint PrivateExtractIcons(string lpszFile, int nIconIndex, int cxIcon, int cyIcon, IntPtr[] phicon, uint[] piconid, uint nIcons, uint flags);
    [DllImport("user32.dll")]
    static extern bool DestroyIcon(IntPtr hIcon);

    public static byte[] Icon(AppEntry a)
    {
        lock (icons)
        {
            byte[] png;
            if (icons.TryGetValue(a.Id, out png)) return png;
            try
            {
                IntPtr[] phicon = new IntPtr[1];
                uint[] piconid = new uint[1];
                uint count = PrivateExtractIcons(a.Path, 0, 128, 128, phicon, piconid, 1, 0);
                if (count > 0 && phicon[0] != IntPtr.Zero)
                {
                    using (var ico = System.Drawing.Icon.FromHandle(phicon[0]))
                    using (var src = ico.ToBitmap())
                    using (var bmp = new Bitmap(128, 128))
                    using (var g = Graphics.FromImage(bmp))
                    {
                        g.InterpolationMode = System.Drawing.Drawing2D.InterpolationMode.HighQualityBicubic;
                        g.DrawImage(src, 0, 0, 128, 128);
                        using (var ms = new MemoryStream()) { bmp.Save(ms, ImageFormat.Png); png = ms.ToArray(); }
                    }
                    DestroyIcon(phicon[0]);
                }
                else
                {
                    using (var ico = System.Drawing.Icon.ExtractAssociatedIcon(a.Path))
                    using (var src = ico.ToBitmap())
                    using (var bmp = new Bitmap(128, 128))
                    using (var g = Graphics.FromImage(bmp))
                    {
                        g.InterpolationMode = System.Drawing.Drawing2D.InterpolationMode.HighQualityBicubic;
                        g.DrawImage(src, 0, 0, 128, 128);
                        using (var ms = new MemoryStream()) { bmp.Save(ms, ImageFormat.Png); png = ms.ToArray(); }
                    }
                }
            }
            catch { png = new byte[0]; }
            icons[a.Id] = png;
            return png;
        }
    }

    public static void Launch(AppEntry a)
    {
        var psi = new ProcessStartInfo(a.Path);
        psi.UseShellExecute = true;
        Process.Start(psi);
    }
}

// ----------------- Core Audio Volume API (COM Interop) -----------------
[ComImport]
[Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
class MMDeviceEnumeratorComObject { }

[Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface IMMDeviceEnumerator
{
    int EnumAudioEndpoints(int dataFlow, int stateMask, out IntPtr devices);
    int GetDefaultAudioEndpoint(int dataFlow, int role, out IMMDevice endpoint);
}

[Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface IMMDevice
{
    int Activate(ref Guid iid, int clsCtx, IntPtr activationParams, [MarshalAs(UnmanagedType.IUnknown)] out object interfacePointer);
}

[Guid("5CDF2C82-841E-4546-9722-0CF74078229A"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
interface IAudioEndpointVolume
{
    int RegisterControlChangeNotify(IntPtr notify);
    int UnregisterControlChangeNotify(IntPtr notify);
    int GetChannelCount(out uint channelCount);
    int SetMasterVolumeLevel(float levelDB, ref Guid eventContext);
    int SetMasterVolumeLevelScalar(float level, ref Guid eventContext);
    int GetMasterVolumeLevel(out float levelDB);
    int GetMasterVolumeLevelScalar(out float level);
    int SetChannelVolumeLevel(uint channelNumber, float levelDB, ref Guid eventContext);
    int SetChannelVolumeLevelScalar(uint channelNumber, float level, ref Guid eventContext);
    int GetChannelVolumeLevel(uint channelNumber, out float levelDB);
    int GetChannelVolumeLevelScalar(uint channelNumber, out float level);
    int SetMute([MarshalAs(UnmanagedType.Bool)] bool isMuted, ref Guid eventContext);
    int GetMute([MarshalAs(UnmanagedType.Bool)] out bool isMuted);
    int GetVolumeStepInfo(out uint step, out uint stepCount);
    int VolumeStepUp(ref Guid eventContext);
    int VolumeStepDown(ref Guid eventContext);
    int QueryHardwareSupport(out uint hardwareSupportMask);
    int GetVolumeRange(out float volumeMinDB, out float volumeMaxDB, out float volumeIncrementDB);
}

static class Audio
{
    static IAudioEndpointVolume cachedVolume;

    static IAudioEndpointVolume GetVolume()
    {
        if (cachedVolume != null) return cachedVolume;
        var enumerator = (IMMDeviceEnumerator)new MMDeviceEnumeratorComObject();
        IMMDevice device;
        enumerator.GetDefaultAudioEndpoint(0, 1, out device);
        var iid = new Guid("5CDF2C82-841E-4546-9722-0CF74078229A");
        object o;
        device.Activate(ref iid, 1, IntPtr.Zero, out o);
        cachedVolume = (IAudioEndpointVolume)o;
        return cachedVolume;
    }

    public static bool GetState(out int volumePercent, out bool muted)
    {
        try
        {
            var vol = GetVolume();
            float level;
            vol.GetMasterVolumeLevelScalar(out level);
            vol.GetMute(out muted);
            volumePercent = (int)Math.Round(level * 100f);
            return true;
        }
        catch
        {
            cachedVolume = null;
            volumePercent = 50;
            muted = false;
            return false;
        }
    }

    public static bool SetVolume(int percent)
    {
        try
        {
            var vol = GetVolume();
            float level = Math.Max(0f, Math.Min(100f, percent)) / 100f;
            var g = Guid.Empty;
            vol.SetMasterVolumeLevelScalar(level, ref g);
            return true;
        }
        catch { cachedVolume = null; return false; }
    }

    public static bool SetMute(bool? targetMute)
    {
        try
        {
            var vol = GetVolume();
            bool current;
            vol.GetMute(out current);
            bool val = targetMute.HasValue ? targetMute.Value : !current;
            var g = Guid.Empty;
            vol.SetMute(val, ref g);
            return true;
        }
        catch { cachedVolume = null; return false; }
    }
}

// ----------------- Keys & Mouse Control -----------------
static class Keys
{
    [DllImport("user32.dll")] static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);

    static readonly Dictionary<string, byte> map = new Dictionary<string, byte>(StringComparer.OrdinalIgnoreCase) {
        { "playpause", 0xB3 }, { "next", 0xB0 }, { "prev", 0xB1 }, { "stop", 0xB2 },
        { "volup", 0xAF }, { "voldown", 0xAE }, { "mute", 0xAD },
        { "enter", 0x0D }, { "esc", 0x1B }, { "space", 0x20 }, { "backspace", 0x08 },
        { "tab", 0x09 }, { "up", 0x26 }, { "down", 0x28 }, { "left", 0x25 }, { "right", 0x27 }
    };

    public static bool Press(string name)
    {
        byte vk;
        if (!map.TryGetValue(name, out vk)) return false;
        // Media and navigation keys require KEYEVENTF_EXTENDEDKEY (0x1)
        bool extended = vk >= 0xA0; // VK_LSHIFT and above, including media keys
        uint downFlag = extended ? 1u : 0u;
        uint upFlag = extended ? 3u : 2u; // KEYEVENTF_KEYUP = 0x2
        keybd_event(vk, 0, downFlag, UIntPtr.Zero);
        keybd_event(vk, 0, upFlag, UIntPtr.Zero);
        return true;
    }

    public static void SendWinD()
    {
        // Win + D (Toggle Desktop)
        keybd_event(0x5B, 0, 1, UIntPtr.Zero); // LWIN down
        keybd_event(0x44, 0, 0, UIntPtr.Zero); // D down
        keybd_event(0x44, 0, 2, UIntPtr.Zero); // D up
        keybd_event(0x5B, 0, 2, UIntPtr.Zero); // LWIN up
    }

    public static void SendAltTab()
    {
        // Alt + Tab
        keybd_event(0x12, 0, 1, UIntPtr.Zero); // ALT down
        keybd_event(0x09, 0, 0, UIntPtr.Zero); // TAB down
        keybd_event(0x09, 0, 2, UIntPtr.Zero); // TAB up
        keybd_event(0x12, 0, 2, UIntPtr.Zero); // ALT up
    }
}

static class Mouse
{
    [DllImport("user32.dll")] static extern void mouse_event(uint flags, int dx, int dy, int data, UIntPtr extra);
    [DllImport("user32.dll")] static extern bool GetCursorPos(out POINT lpPoint);
    [DllImport("user32.dll")] static extern bool SetCursorPos(int X, int Y);

    [StructLayout(LayoutKind.Sequential)]
    struct POINT { public int X; public int Y; }

    const uint MOUSEEVENTF_MOVE = 0x0001;
    const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    const uint MOUSEEVENTF_LEFTUP = 0x0004;
    const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    const uint MOUSEEVENTF_RIGHTUP = 0x0010;
    const uint MOUSEEVENTF_MIDDLEDOWN = 0x0020;
    const uint MOUSEEVENTF_MIDDLEUP = 0x0040;
    const uint MOUSEEVENTF_WHEEL = 0x0800;

    public static void Move(int dx, int dy)
    {
        mouse_event(MOUSEEVENTF_MOVE, dx, dy, 0, UIntPtr.Zero);
    }

    public static void Click(string btn)
    {
        if (btn == "right")
        {
            mouse_event(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, UIntPtr.Zero);
            mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, UIntPtr.Zero);
        }
        else if (btn == "middle")
        {
            mouse_event(MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, UIntPtr.Zero);
            mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, UIntPtr.Zero);
        }
        else if (btn == "double")
        {
            Click("left");
            Thread.Sleep(50);
            Click("left");
        }
        else
        {
            mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, UIntPtr.Zero);
            mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
        }
    }

    public static void ReleaseAll()
    {
        mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
        mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, UIntPtr.Zero);
        mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, UIntPtr.Zero);
    }

    public static void Down(string btn)
    {
        uint f = btn == "right" ? MOUSEEVENTF_RIGHTDOWN : btn == "middle" ? MOUSEEVENTF_MIDDLEDOWN : MOUSEEVENTF_LEFTDOWN;
        mouse_event(f, 0, 0, 0, UIntPtr.Zero);
    }

    public static void Up(string btn)
    {
        if (btn == "all") { ReleaseAll(); return; }
        uint f = btn == "right" ? MOUSEEVENTF_RIGHTUP : btn == "middle" ? MOUSEEVENTF_MIDDLEUP : MOUSEEVENTF_LEFTUP;
        mouse_event(f, 0, 0, 0, UIntPtr.Zero);
    }

    public static void Scroll(int delta)
    {
        mouse_event(MOUSEEVENTF_WHEEL, 0, 0, delta, UIntPtr.Zero);
    }
}

static class SystemPower
{
    [DllImport("user32.dll")] static extern bool LockWorkStation();
    [DllImport("PowrProf.dll", SetLastError = true)] static extern bool SetSuspendState(bool hibernate, bool forceCritical, bool disableWakeEvent);
    [DllImport("user32.dll")] static extern IntPtr SendMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);

    const int HWND_BROADCAST = 0xFFFF;
    const uint WM_SYSCOMMAND = 0x0112;
    static readonly IntPtr SC_MONITORPOWER = new IntPtr(0xF170);

    public static void Execute(string action)
    {
        switch (action.ToLowerInvariant())
        {
            case "lock":
                LockWorkStation();
                break;
            case "sleep":
                SetSuspendState(false, true, false);
                break;
            case "monitors_off":
                SendMessage(new IntPtr(HWND_BROADCAST), WM_SYSCOMMAND, SC_MONITORPOWER, new IntPtr(2));
                break;
            case "desktop":
                Keys.SendWinD();
                break;
            case "alttab":
                Keys.SendAltTab();
                break;
            case "shutdown":
                Process.Start("shutdown", "/s /t 30 /c \"Pult Remote Shutdown\"");
                break;
            case "restart":
                Process.Start("shutdown", "/r /t 30 /c \"Pult Remote Restart\"");
                break;
            case "cancel_shutdown":
                Process.Start("shutdown", "/a");
                break;
        }
    }
}

class WindowItem
{
    public IntPtr Hwnd;
    public string Title;
    public string ProcessName;
}

static class WindowTasks
{
    delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
    [DllImport("user32.dll")] static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);
    [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr hWnd);
    [DllImport("user32.dll")] static extern int GetWindowText(IntPtr hWnd, StringBuilder strText, int maxCount);
    [DllImport("user32.dll")] static extern int GetWindowTextLength(IntPtr hWnd);
    [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);
    [DllImport("user32.dll")] static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] static extern IntPtr SendMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);

    const int SW_RESTORE = 9;
    const uint WM_CLOSE = 0x0010;

    public static List<WindowItem> List()
    {
        var list = new List<WindowItem>();
        try
        {
            EnumWindows(delegate(IntPtr hWnd, IntPtr lParam)
            {
                if (!IsWindowVisible(hWnd)) return true;
                int len = GetWindowTextLength(hWnd);
                if (len == 0) return true;
                var sb = new StringBuilder(len + 1);
                GetWindowText(hWnd, sb, sb.Capacity);
                var title = sb.ToString().Trim();
                if (string.IsNullOrEmpty(title)) return true;
                if (title == "Program Manager" || title == "Windows Input Experience" || title == "Settings") return true;

                uint pid;
                GetWindowThreadProcessId(hWnd, out pid);
                string proc = "";
                try { proc = Process.GetProcessById((int)pid).ProcessName; } catch { }

                list.Add(new WindowItem { Hwnd = hWnd, Title = title, ProcessName = proc });
                return true;
            }, IntPtr.Zero);
        }
        catch { }
        return list;
    }

    public static void Switch(IntPtr hwnd)
    {
        ShowWindow(hwnd, SW_RESTORE);
        SetForegroundWindow(hwnd);
    }

    public static void Close(IntPtr hwnd)
    {
        SendMessage(hwnd, WM_CLOSE, IntPtr.Zero, IntPtr.Zero);
    }
}

// ----------------- Clipboard (Thread-Safe STA) -----------------
static class Clip
{
    public static string GetText()
    {
        string text = "";
        var t = new Thread(delegate()
        {
            try { if (Clipboard.ContainsText()) text = Clipboard.GetText(); }
            catch { }
        });
        t.SetApartmentState(ApartmentState.STA);
        t.Start();
        t.Join(800);
        return text ?? "";
    }

    public static bool SetText(string text)
    {
        bool ok = false;
        var t = new Thread(delegate()
        {
            try
            {
                if (string.IsNullOrEmpty(text)) Clipboard.Clear();
                else Clipboard.SetText(text);
                ok = true;
            }
            catch { }
        });
        t.SetApartmentState(ApartmentState.STA);
        t.Start();
        t.Join(800);
        return ok;
    }

    public static bool Clear()
    {
        bool ok = false;
        var t = new Thread(delegate()
        {
            try { Clipboard.Clear(); ok = true; } catch { }
        });
        t.SetApartmentState(ApartmentState.STA);
        t.Start();
        t.Join(800);
        return ok;
    }
}

// ----------------- Desktop Screen Capture -----------------
static class ScreenCapture
{
    public static byte[] CaptureDesktop(bool saveToDisk, bool copyToClipboard, out string savedPath)
    {
        savedPath = null;
        try
        {
            var bounds = Screen.PrimaryScreen.Bounds;
            if (bounds.Width <= 0 || bounds.Height <= 0) bounds = new Rectangle(0, 0, 1920, 1080);

            using (var bmp = new Bitmap(bounds.Width, bounds.Height, PixelFormat.Format32bppArgb))
            {
                bool captured = false;
                try
                {
                    using (var g = Graphics.FromImage(bmp))
                    {
                        g.CopyFromScreen(bounds.X, bounds.Y, 0, 0, bounds.Size, CopyPixelOperation.SourceCopy);
                        captured = true;
                    }
                }
                catch
                {
                    // Fallback for locked desktop / background session
                    using (var g = Graphics.FromImage(bmp))
                    {
                        g.Clear(Color.FromArgb(16, 23, 25));
                        using (var f = new Font("Segoe UI", 18f, FontStyle.Bold))
                        using (var b = new SolidBrush(Color.FromArgb(180, 237, 204)))
                        {
                            g.DrawString("PULT DESKTOP SCREEN", f, b, new PointF(40, 40));
                        }
                        using (var f2 = new Font("Segoe UI", 11f, FontStyle.Regular))
                        using (var b2 = new SolidBrush(Color.FromArgb(160, 175, 175)))
                        {
                            g.DrawString("Экран Windows заблокирован или сессия неактивна (" + DateTime.Now.ToString("HH:mm:ss") + ")", f2, b2, new PointF(40, 80));
                        }
                        captured = true;
                    }
                }

                if (copyToClipboard && captured)
                {
                    var copyBmp = (Bitmap)bmp.Clone();
                    var t = new Thread(delegate()
                    {
                        try { Clipboard.SetImage(copyBmp); } catch { }
                        finally { copyBmp.Dispose(); }
                    });
                    t.SetApartmentState(ApartmentState.STA);
                    t.Start();
                    t.Join(1000);
                }

                if (saveToDisk && captured)
                {
                    var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyPictures), "Pult");
                    Directory.CreateDirectory(dir);
                    var file = "Pult_Capture_" + DateTime.Now.ToString("yyyyMMdd_HHmmss") + ".png";
                    savedPath = Path.Combine(dir, file);
                    bmp.Save(savedPath, ImageFormat.Png);
                }

                using (var ms = new MemoryStream())
                {
                    bmp.Save(ms, ImageFormat.Jpeg);
                    return ms.ToArray();
                }
            }
        }
        catch { return new byte[0]; }
    }
}

// ----------------- Signature Receiver -----------------
static class SignatureHandler
{
    public static bool Handle(byte[] pngBytes, string action, out string savedPath)
    {
        savedPath = null;
        if (pngBytes == null || pngBytes.Length == 0) return false;
        bool ok = false;
        string path = null;
        var t = new Thread(delegate()
        {
            try
            {
                using (var ms = new MemoryStream(pngBytes))
                using (var img = Image.FromStream(ms))
                using (var bmp = new Bitmap(img))
                {
                    if (action == "clipboard" || action == "all")
                    {
                        Clipboard.SetImage(bmp);
                        ok = true;
                    }
                    if (action == "save" || action == "all")
                    {
                        var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyPictures), "Pult", "Signatures");
                        Directory.CreateDirectory(dir);
                        var file = "Signature_" + DateTime.Now.ToString("yyyyMMdd_HHmmss") + ".png";
                        path = Path.Combine(dir, file);
                        bmp.Save(path, ImageFormat.Png);
                        ok = true;
                    }
                }
            } catch { }
        });
        t.SetApartmentState(ApartmentState.STA);
        t.Start();
        t.Join(2000);
        savedPath = path;
        return ok;
    }
}

// ----------------- Fluent Slider Control -----------------
public class FluentSlider : Control
{
    int val = 50;
    bool dragging = false;
    public event EventHandler ValueChanged;

    public int Value
    {
        get { return val; }
        set
        {
            int c = Math.Max(0, Math.Min(100, value));
            if (val != c)
            {
                val = c;
                Invalidate();
                if (ValueChanged != null) ValueChanged(this, EventArgs.Empty);
            }
        }
    }

    public FluentSlider()
    {
        SetStyle(ControlStyles.AllPaintingInWmPaint | ControlStyles.UserPaint | ControlStyles.OptimizedDoubleBuffer | ControlStyles.ResizeRedraw | ControlStyles.SupportsTransparentBackColor, true);
        Height = 18;
        Cursor = Cursors.Hand;
        BackColor = Color.FromArgb(31, 31, 31);
    }

    protected override void OnMouseDown(MouseEventArgs e)
    {
        base.OnMouseDown(e);
        if (e.Button == MouseButtons.Left)
        {
            dragging = true;
            Capture = true;
            UpdateFromX(e.X);
        }
    }

    protected override void OnMouseMove(MouseEventArgs e)
    {
        base.OnMouseMove(e);
        if (dragging) UpdateFromX(e.X);
    }

    protected override void OnMouseUp(MouseEventArgs e)
    {
        base.OnMouseUp(e);
        dragging = false;
        Capture = false;
    }

    void UpdateFromX(int x)
    {
        int pad = 6;
        int trackW = Width - pad * 2;
        if (trackW <= 0) return;
        float r = (float)(x - pad) / trackW;
        Value = (int)Math.Round(r * 100);
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        var g = e.Graphics;
        g.SmoothingMode = System.Drawing.Drawing2D.SmoothingMode.AntiAlias;

        int pad = 6;
        int trackW = Width - pad * 2;
        int midY = Height / 2;
        int railH = 4;
        int railY = midY - railH / 2;

        // Inactive rail
        using (var bgBrush = new SolidBrush(Color.FromArgb(70, 70, 70)))
        using (var path = CreateRoundRect(pad, railY, trackW, railH, 2))
        {
            g.FillPath(bgBrush, path);
        }

        // Active rail
        int activeW = (int)(trackW * (val / 100.0f));
        if (activeW > 0)
        {
            using (var actBrush = new SolidBrush(Color.FromArgb(56, 189, 248)))
            using (var path = CreateRoundRect(pad, railY, activeW, railH, 2))
            {
                g.FillPath(actBrush, path);
            }
        }

        // Circular thumb
        int thumbX = pad + activeW;
        int thumbR = 7;
        using (var borderBrush = new SolidBrush(Color.FromArgb(34, 34, 34)))
        using (var thumbBrush = new SolidBrush(Color.FromArgb(56, 189, 248)))
        {
            g.FillEllipse(borderBrush, thumbX - thumbR - 1, midY - thumbR - 1, (thumbR + 1) * 2, (thumbR + 1) * 2);
            g.FillEllipse(thumbBrush, thumbX - thumbR, midY - thumbR, thumbR * 2, thumbR * 2);
        }
    }

    static System.Drawing.Drawing2D.GraphicsPath CreateRoundRect(int x, int y, int w, int h, int r)
    {
        var path = new System.Drawing.Drawing2D.GraphicsPath();
        if (w <= 0 || h <= 0) return path;
        int d = r * 2;
        if (d > w) d = w;
        if (d > h) d = h;
        path.AddArc(x, y, d, d, 180, 90);
        path.AddArc(x + w - d, y, d, d, 270, 90);
        path.AddArc(x + w - d, y + h - d, d, d, 0, 90);
        path.AddArc(x, y + h - d, d, d, 90, 90);
        path.CloseFigure();
        return path;
    }
}

// ----------------- Windows 11 Native Mica Companion Form -----------------
public class CompanionFlyout : Form
{
    [DllImport("dwmapi.dll")] static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int attrValue, int attrSize);

    const int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    const int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    const int DWMWA_SYSTEMBACKDROP_TYPE = 38;
    const int DWMWA_MICA_EFFECT = 1029;

    public CompanionFlyout()
    {
        this.FormBorderStyle = FormBorderStyle.None;
        this.StartPosition = FormStartPosition.Manual;
        this.ShowInTaskbar = false;
        this.TopMost = true;
        this.Width = 320;
        this.Height = 330;
        this.BackColor = Color.FromArgb(28, 28, 28);
        this.ForeColor = Color.White;
        this.Font = new Font("Segoe UI", 9.5f, FontStyle.Regular);

        var area = Screen.PrimaryScreen.WorkingArea;
        this.Location = new Point(area.Right - Width - 14, area.Bottom - Height - 14);

        BuildUi();
        this.Deactivate += delegate { this.Close(); };
    }

    protected override void OnHandleCreated(EventArgs e)
    {
        base.OnHandleCreated(e);
        try
        {
            int dark = 1;
            DwmSetWindowAttribute(Handle, DWMWA_USE_IMMERSIVE_DARK_MODE, ref dark, sizeof(int));
            int round = 2; // DWMWCP_ROUND
            DwmSetWindowAttribute(Handle, DWMWA_WINDOW_CORNER_PREFERENCE, ref round, sizeof(int));

            int osBuild = Environment.OSVersion.Version.Build;
            if (osBuild >= 22621)
            {
                int backdrop = 2; // Mica
                DwmSetWindowAttribute(Handle, DWMWA_SYSTEMBACKDROP_TYPE, ref backdrop, sizeof(int));
            }
            else if (osBuild >= 22000)
            {
                int mica = 1;
                DwmSetWindowAttribute(Handle, DWMWA_MICA_EFFECT, ref mica, sizeof(int));
            }
        }
        catch { }
    }

    protected override void OnPaint(PaintEventArgs e)
    {
        base.OnPaint(e);
        var g = e.Graphics;

        // Outer border 1px
        using (var borderPen = new Pen(Color.FromArgb(44, 44, 44), 1))
        {
            g.DrawRectangle(borderPen, 0, 0, Width - 1, Height - 1);
        }

        // Active cyan indicator bar on the left of Master Volume row
        using (var barBrush = new SolidBrush(Color.FromArgb(56, 189, 248)))
        {
            g.FillRectangle(barBrush, 4, 12, 3, 20);
        }

        // Separator lines
        using (var sepPen = new Pen(Color.FromArgb(38, 38, 38), 1))
        {
            g.DrawLine(sepPen, 10, 56, Width - 10, 56);
            g.DrawLine(sepPen, 10, 112, Width - 10, 112);
            g.DrawLine(sepPen, 10, 168, Width - 10, 168);
            g.DrawLine(sepPen, 10, 224, Width - 10, 224);
            g.DrawLine(sepPen, 0, 280, Width, 280);
        }

        // Bottom footer background
        using (var footBrush = new SolidBrush(Color.FromArgb(22, 22, 22)))
        {
            g.FillRectangle(footBrush, 0, 281, Width, Height - 281);
        }
    }

    void BuildUi()
    {
        Font fontMdl2 = null;
        try { fontMdl2 = new Font("Segoe MDL2 Assets", 12f, FontStyle.Regular); } catch { }
        if (fontMdl2 == null || fontMdl2.FontFamily.Name != "Segoe MDL2 Assets")
        {
            fontMdl2 = new Font("Segoe UI Symbol", 11f, FontStyle.Regular);
        }

        // --- ROW 1: Speakers (Realtek(R) Audio) ---
        int curVol; bool curMute;
        Audio.GetState(out curVol, out curMute);

        var lblSpeakerIcon = new Label {
            Text = "\uE767",
            Font = fontMdl2,
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(14, 12),
            Size = new Size(20, 22),
            BackColor = Color.Transparent
        };
        Controls.Add(lblSpeakerIcon);

        var lblDeviceName = new Label {
            Text = "Speakers (Realtek(R) Audio)",
            Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(38, 8),
            AutoSize = true,
            BackColor = Color.Transparent
        };
        Controls.Add(lblDeviceName);

        var lblVolVal = new Label {
            Text = curVol.ToString(),
            Font = new Font("Segoe UI", 10.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(278, 24),
            Size = new Size(36, 20),
            TextAlign = ContentAlignment.MiddleRight,
            BackColor = Color.Transparent
        };
        Controls.Add(lblVolVal);

        var sliderVol = new FluentSlider {
            Location = new Point(36, 26),
            Size = new Size(236, 18),
            Value = curVol
        };
        sliderVol.ValueChanged += delegate {
            Audio.SetVolume(sliderVol.Value);
            lblVolVal.Text = sliderVol.Value.ToString();
        };
        Controls.Add(sliderVol);

        // --- ROW 2: Brave Browser ---
        Image braveImg = null;
        try {
            var iconPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, @"..\assets\app-icons\brave.png");
            if (File.Exists(iconPath)) braveImg = Image.FromFile(iconPath);
        } catch { }

        if (braveImg != null)
        {
            var pbBrave = new PictureBox {
                Image = braveImg,
                SizeMode = PictureBoxSizeMode.Zoom,
                Location = new Point(14, 68),
                Size = new Size(20, 20),
                BackColor = Color.Transparent
            };
            Controls.Add(pbBrave);
        }
        else
        {
            var lblBraveIcon = new Label {
                Text = "\uE774",
                Font = fontMdl2,
                ForeColor = Color.FromArgb(255, 102, 0),
                Location = new Point(14, 68),
                Size = new Size(20, 22),
                BackColor = Color.Transparent
            };
            Controls.Add(lblBraveIcon);
        }

        var lblBraveTitle = new Label {
            Text = "Brave Browser",
            Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(38, 64),
            AutoSize = true,
            BackColor = Color.Transparent
        };
        Controls.Add(lblBraveTitle);

        var lblBraveVal = new Label {
            Text = "100",
            Font = new Font("Segoe UI", 10.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(278, 80),
            Size = new Size(36, 20),
            TextAlign = ContentAlignment.MiddleRight,
            BackColor = Color.Transparent
        };
        Controls.Add(lblBraveVal);

        var sliderBrave = new FluentSlider {
            Location = new Point(36, 82),
            Size = new Size(236, 18),
            Value = 100
        };
        sliderBrave.ValueChanged += delegate {
            lblBraveVal.Text = sliderBrave.Value.ToString();
        };
        Controls.Add(sliderBrave);

        // --- ROW 3: MAG 271QP X28 (Monitor Brightness) ---
        var lblMonIcon = new Label {
            Text = "\uE706",
            Font = fontMdl2,
            ForeColor = Color.FromArgb(220, 220, 220),
            Location = new Point(14, 124),
            Size = new Size(20, 22),
            BackColor = Color.Transparent
        };
        Controls.Add(lblMonIcon);

        var lblMonTitle = new Label {
            Text = "MAG 271QP X28",
            Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(38, 120),
            AutoSize = true,
            BackColor = Color.Transparent
        };
        Controls.Add(lblMonTitle);

        var lblMonVal = new Label {
            Text = "100",
            Font = new Font("Segoe UI", 10.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(278, 136),
            Size = new Size(36, 20),
            TextAlign = ContentAlignment.MiddleRight,
            BackColor = Color.Transparent
        };
        Controls.Add(lblMonVal);

        var sliderMon = new FluentSlider {
            Location = new Point(36, 138),
            Size = new Size(236, 18),
            Value = 100
        };
        sliderMon.ValueChanged += delegate {
            lblMonVal.Text = sliderMon.Value.ToString();
        };
        Controls.Add(sliderMon);

        // --- ROW 4: Фильтр синего света ---
        var lblMoonIcon = new Label {
            Text = "\uE708",
            Font = fontMdl2,
            ForeColor = Color.FromArgb(220, 220, 220),
            Location = new Point(14, 180),
            Size = new Size(20, 22),
            BackColor = Color.Transparent
        };
        Controls.Add(lblMoonIcon);

        var lblMoonTitle = new Label {
            Text = "Фильтр синего света",
            Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(38, 176),
            AutoSize = true,
            BackColor = Color.Transparent
        };
        Controls.Add(lblMoonTitle);

        var lblMoonVal = new Label {
            Text = "0",
            Font = new Font("Segoe UI", 10.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(278, 192),
            Size = new Size(36, 20),
            TextAlign = ContentAlignment.MiddleRight,
            BackColor = Color.Transparent
        };
        Controls.Add(lblMoonVal);

        var sliderMoon = new FluentSlider {
            Location = new Point(36, 194),
            Size = new Size(236, 18),
            Value = 0
        };
        sliderMoon.ValueChanged += delegate {
            lblMoonVal.Text = sliderMoon.Value.ToString();
        };
        Controls.Add(sliderMoon);

        // --- ROW 5: Затемнение ---
        var lblDimIcon = new Label {
            Text = "\uE706",
            Font = fontMdl2,
            ForeColor = Color.FromArgb(220, 220, 220),
            Location = new Point(14, 236),
            Size = new Size(20, 22),
            BackColor = Color.Transparent
        };
        Controls.Add(lblDimIcon);

        var lblDimTitle = new Label {
            Text = "Затемнение",
            Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(38, 232),
            AutoSize = true,
            BackColor = Color.Transparent
        };
        Controls.Add(lblDimTitle);

        var lblDimVal = new Label {
            Text = "0",
            Font = new Font("Segoe UI", 10.5f, FontStyle.Regular),
            ForeColor = Color.FromArgb(240, 240, 240),
            Location = new Point(278, 248),
            Size = new Size(36, 20),
            TextAlign = ContentAlignment.MiddleRight,
            BackColor = Color.Transparent
        };
        Controls.Add(lblDimVal);

        var sliderDim = new FluentSlider {
            Location = new Point(36, 250),
            Size = new Size(236, 18),
            Value = 0
        };
        sliderDim.ValueChanged += delegate {
            lblDimVal.Text = sliderDim.Value.ToString();
        };
        Controls.Add(sliderDim);

        // --- BOTTOM FOOTER BAR ---
        var lblFooterTitle = new Label {
            Text = "Громкость",
            Font = new Font("Segoe UI", 10f, FontStyle.Regular),
            ForeColor = Color.FromArgb(230, 230, 230),
            Location = new Point(14, 294),
            AutoSize = true,
            BackColor = Color.Transparent
        };
        Controls.Add(lblFooterTitle);

        // Equalizer / Mixer button
        var btnMixer = new Button {
            Text = "\uE994",
            Font = fontMdl2,
            ForeColor = Color.FromArgb(220, 220, 220),
            BackColor = Color.FromArgb(22, 22, 22),
            FlatStyle = FlatStyle.Flat,
            Location = new Point(242, 288),
            Size = new Size(32, 32),
            Cursor = Cursors.Hand
        };
        btnMixer.FlatAppearance.BorderSize = 0;
        btnMixer.Click += delegate {
            try { Process.Start("ms-settings:apps-volume"); }
            catch { Firewall.RunElevated(); }
        };
        Controls.Add(btnMixer);

        // Settings / Pult Companion Cog
        var btnSettings = new Button {
            Text = "\uE713",
            Font = fontMdl2,
            ForeColor = Color.FromArgb(220, 220, 220),
            BackColor = Color.FromArgb(22, 22, 22),
            FlatStyle = FlatStyle.Flat,
            Location = new Point(278, 288),
            Size = new Size(32, 32),
            Cursor = Cursors.Hand
        };
        btnSettings.FlatAppearance.BorderSize = 0;

        var menu = new ContextMenuStrip();
        bool isConnected = Program.LastClientIp != null && (DateTime.Now - Program.LastActiveTime).TotalSeconds < 25;
        var firstIp = Program.LocalIps().FirstOrDefault() ?? "127.0.0.1";

        menu.Items.Add(new ToolStripMenuItem("Pult Companion Status: " + (isConnected ? "Подключен (" + Program.LastClientIp + ")" : "Ожидание смартфона...")) { Enabled = false });
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("PIN: " + Program.Pin + " (Скопировать)", null, delegate {
            Clipboard.SetText(Program.Pin);
            Program.Notify("PIN скопирован: " + Program.Pin);
        });
        menu.Items.Add("IP хоста: " + firstIp + ":" + Program.HttpPort + " (Скопировать)", null, delegate {
            Clipboard.SetText(firstIp);
            Program.Notify("IP скопирован: " + firstIp);
        });
        menu.Items.Add("Сгенерировать новый PIN", null, delegate {
            Program.Pin = Program.NewPin();
            Program.Notify("Новый PIN: " + Program.Pin);
        });
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("Открыть папку снимков", null, delegate {
            var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyPictures), "Pult");
            Directory.CreateDirectory(dir);
            Process.Start("explorer.exe", dir);
        });
        menu.Items.Add("Открыть папку подписей", null, delegate {
            var dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyPictures), "Pult", "Signatures");
            Directory.CreateDirectory(dir);
            Process.Start("explorer.exe", dir);
        });
        menu.Items.Add("Разрешить в брандмауэре Windows", null, delegate { Firewall.RunElevated(); });
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add("Выход из Pult", null, delegate { Environment.Exit(0); });

        btnSettings.Click += delegate {
            menu.Show(btnSettings, new Point(-160, btnSettings.Height));
        };
        Controls.Add(btnSettings);
    }
}

// ----------------- HTTP Server -----------------
static class Server
{
    static int failures;
    static DateTime lockUntil;

    public static void Start()
    {
        var l = new TcpListener(IPAddress.Any, Program.HttpPort);
        try { l.Start(); }
        catch (SocketException)
        {
            MessageBox.Show("Порт " + Program.HttpPort + " занят.", "Pult");
            Environment.Exit(1);
        }
        var t = new Thread(delegate()
        {
            while (true)
            {
                try
                {
                    var c = l.AcceptTcpClient();
                    ThreadPool.QueueUserWorkItem(delegate { Handle(c); });
                }
                catch { }
            }
        });
        t.IsBackground = true;
        t.Start();
    }

    static void Handle(TcpClient c)
    {
        using (c)
        {
            try
            {
                c.ReceiveTimeout = 15000;
                c.SendTimeout = 6000;
                c.NoDelay = true;
                var s = c.GetStream();
                while (true)
                {
                    var head = ReadHead(s);
                    if (head == null) break;
                    var lines = head.Split(new[] { "\r\n" }, StringSplitOptions.None);
                    var parts = lines[0].Split(' ');
                    if (parts.Length < 2) break;
                    var method = parts[0];
                    var target = parts[1];
                    var q = target.IndexOf('?');
                    var path = q < 0 ? target : target.Substring(0, q);
                    var query = ParseQuery(q < 0 ? "" : target.Substring(q + 1));
                    string pin = null;
                    bool closeReq = false;
                    foreach (var h in lines.Skip(1))
                    {
                        if (h.StartsWith("X-Pin:", StringComparison.OrdinalIgnoreCase)) pin = h.Substring(6).Trim();
                        if (h.StartsWith("Connection:", StringComparison.OrdinalIgnoreCase) && h.IndexOf("close", StringComparison.OrdinalIgnoreCase) >= 0) closeReq = true;
                    }
                    if (pin == null) query.TryGetValue("pin", out pin);

                    if (path == "/ping")
                    {
                        int vol; bool muted;
                        Audio.GetState(out vol, out muted);
                        Send(s, 200, "application/json", Json("{\"app\":\"pult\",\"name\":" + Q(Environment.MachineName) +
                            ",\"port\":" + Program.HttpPort + ",\"version\":\"0.2\",\"volume\":" + vol + ",\"muted\":" + (muted ? "true" : "false") + "}"), !closeReq);
                        if (closeReq) break;
                        continue;
                    }
                    if (!Authorized(pin))
                    {
                        Send(s, 401, "application/json", Json("{\"error\":\"pin\"}"), false);
                        break;
                    }
                    Program.OnClientActivity(c);

                    if (path == "/apps")
                    {
                        var sb = new StringBuilder("[");
                        var list = Apps.List();
                        for (int i = 0; i < list.Count; i++)
                        {
                            if (i > 0) sb.Append(',');
                            sb.Append("{\"id\":" + Q(list[i].Id) + ",\"name\":" + Q(list[i].Name) + "}");
                        }
                        sb.Append(']');
                        Send(s, 200, "application/json", Json(sb.ToString()), !closeReq);
                    }
                    else if (path == "/icon")
                    {
                        string id; query.TryGetValue("id", out id);
                        var a = Apps.Find(id ?? "");
                        var png = a == null ? new byte[0] : Apps.Icon(a);
                        if (png.Length == 0) Send(s, 404, "text/plain", new byte[0], !closeReq);
                        else Send(s, 200, "image/png", png, !closeReq);
                    }
                    else if (path == "/launch" && method == "POST")
                    {
                        string id; query.TryGetValue("id", out id);
                        var a = Apps.Find(id ?? "");
                        if (a == null) { Send(s, 404, "application/json", Json("{\"error\":\"no app\"}"), !closeReq); if (closeReq) break; continue; }
                        Apps.Launch(a);
                        Send(s, 200, "application/json", Json("{\"ok\":true}"), !closeReq);
                    }
                    else if (path == "/volume")
                    {
                        if (method == "POST")
                        {
                            string setVal;
                            if (query.TryGetValue("set", out setVal))
                            {
                                int val;
                                if (int.TryParse(setVal, out val)) Audio.SetVolume(val);
                            }
                            string muteVal;
                            if (query.TryGetValue("mute", out muteVal))
                            {
                                if (muteVal == "toggle") Audio.SetMute(null);
                                else if (muteVal == "true" || muteVal == "1") Audio.SetMute(true);
                                else if (muteVal == "false" || muteVal == "0") Audio.SetMute(false);
                            }
                            string stepVal;
                            if (query.TryGetValue("step", out stepVal))
                            {
                                int step;
                                if (int.TryParse(stepVal, out step))
                                {
                                    int cur; bool m;
                                    Audio.GetState(out cur, out m);
                                    Audio.SetVolume(cur + step);
                                }
                            }
                        }
                        int v; bool isMuted;
                        Audio.GetState(out v, out isMuted);
                        Send(s, 200, "application/json", Json("{\"volume\":" + v + ",\"muted\":" + (isMuted ? "true" : "false") + ",\"ok\":true}"), !closeReq);
                    }
                    else if (path == "/key" && method == "POST")
                    {
                        string name; query.TryGetValue("name", out name);
                        var ok = Keys.Press(name ?? "");
                        Send(s, ok ? 200 : 400, "application/json", Json("{\"ok\":" + (ok ? "true" : "false") + "}"), !closeReq);
                    }
                    else if (path == "/mouse" && method == "POST")
                    {
                        string dxStr, dyStr;
                        if (query.TryGetValue("dx", out dxStr) && query.TryGetValue("dy", out dyStr))
                        {
                            int dx, dy;
                            if (int.TryParse(dxStr, out dx) && int.TryParse(dyStr, out dy)) Mouse.Move(dx, dy);
                        }
                        string click;
                        if (query.TryGetValue("click", out click)) Mouse.Click(click);
                        string down;
                        if (query.TryGetValue("down", out down)) Mouse.Down(down);
                        string up;
                        if (query.TryGetValue("up", out up)) Mouse.Up(up);
                        string scrollStr;
                        if (query.TryGetValue("scroll", out scrollStr))
                        {
                            int scroll;
                            if (int.TryParse(scrollStr, out scroll)) Mouse.Scroll(scroll);
                        }
                        Send(s, 200, "application/json", Json("{\"ok\":true}"), !closeReq);
                    }
                    else if (path == "/system" && method == "POST")
                    {
                        string action; query.TryGetValue("action", out action);
                        if (!string.IsNullOrEmpty(action)) SystemPower.Execute(action);
                        Send(s, 200, "application/json", Json("{\"ok\":true}"), !closeReq);
                    }
                    else if (path == "/tasks")
                    {
                        var tasks = WindowTasks.List();
                        var sb = new StringBuilder("[");
                        for (int i = 0; i < tasks.Count; i++)
                        {
                            if (i > 0) sb.Append(',');
                            sb.Append("{\"id\":" + tasks[i].Hwnd.ToInt64() + ",\"title\":" + Q(tasks[i].Title) + ",\"proc\":" + Q(tasks[i].ProcessName) + "}");
                        }
                        sb.Append(']');
                        Send(s, 200, "application/json", Json(sb.ToString()), !closeReq);
                    }
                    else if (path == "/tasks/switch" && method == "POST")
                    {
                        string idStr; query.TryGetValue("id", out idStr);
                        long hwnd;
                        if (long.TryParse(idStr, out hwnd)) WindowTasks.Switch(new IntPtr(hwnd));
                        Send(s, 200, "application/json", Json("{\"ok\":true}"), !closeReq);
                    }
                    else if (path == "/tasks/close" && method == "POST")
                    {
                        string idStr; query.TryGetValue("id", out idStr);
                        long hwnd;
                        if (long.TryParse(idStr, out hwnd)) WindowTasks.Close(new IntPtr(hwnd));
                        Send(s, 200, "application/json", Json("{\"ok\":true}"), !closeReq);
                    }
                    else if (path == "/keyboard" && method == "POST")
                    {
                        string key;
                        if (query.TryGetValue("key", out key)) Keys.Press(key);
                        string text;
                        if (query.TryGetValue("text", out text) && !string.IsNullOrEmpty(text))
                        {
                            try { SendKeys.SendWait(text); } catch { }
                        }
                        Send(s, 200, "application/json", Json("{\"ok\":true}"), !closeReq);
                    }
                    else if (path == "/clipboard" && method == "GET")
                    {
                        var text = Clip.GetText();
                        Send(s, 200, "application/json", Json("{\"ok\":true,\"hasText\":" + (string.IsNullOrEmpty(text) ? "false" : "true") + ",\"text\":" + Q(text ?? "") + "}"), !closeReq);
                    }
                    else if (path == "/clipboard" && method == "POST")
                    {
                        if (query.ContainsKey("clear"))
                        {
                            Clip.Clear();
                            Send(s, 200, "application/json", Json("{\"ok\":true}"), !closeReq);
                        }
                        else
                        {
                            string text;
                            query.TryGetValue("text", out text);
                            Clip.SetText(text ?? "");
                            Send(s, 200, "application/json", Json("{\"ok\":true}"), !closeReq);
                        }
                    }
                    else if (path == "/capture" && method == "GET")
                    {
                        string pth;
                        var bytes = ScreenCapture.CaptureDesktop(false, false, out pth);
                        if (bytes == null || bytes.Length == 0) Send(s, 500, "text/plain", new byte[0], !closeReq);
                        else Send(s, 200, "image/jpeg", bytes, !closeReq);
                    }
                    else if (path == "/capture" && method == "POST")
                    {
                        bool save = query.ContainsKey("save");
                        bool copy = query.ContainsKey("copy");
                        string pth;
                        ScreenCapture.CaptureDesktop(save, copy, out pth);
                        Send(s, 200, "application/json", Json("{\"ok\":true,\"path\":" + Q(pth ?? "") + "}"), !closeReq);
                    }
                    else if (path == "/signature" && method == "POST")
                    {
                        string action; query.TryGetValue("action", out action);
                        string data; query.TryGetValue("data", out data);
                        byte[] bytes = null;
                        if (!string.IsNullOrEmpty(data))
                        {
                            try { bytes = Convert.FromBase64String(data); } catch { }
                        }
                        string pth;
                        bool ok = SignatureHandler.Handle(bytes, action ?? "all", out pth);
                        Send(s, ok ? 200 : 400, "application/json", Json("{\"ok\":" + (ok ? "true" : "false") + ",\"path\":" + Q(pth ?? "") + "}"), !closeReq);
                    }
                    else Send(s, 404, "application/json", Json("{\"error\":\"not found\"}"), !closeReq);

                    if (closeReq) break;
                }
            }
            catch { }
            finally
            {
                Mouse.ReleaseAll();
            }
        }
    }

    static bool Authorized(string pin)
    {
        lock (typeof(Server))
        {
            if (DateTime.UtcNow < lockUntil) return false;
            if (pin != null && pin == Program.Pin) { failures = 0; return true; }
            if (++failures >= 10) { failures = 0; lockUntil = DateTime.UtcNow.AddSeconds(60); }
            return false;
        }
    }

    static string ReadHead(NetworkStream s)
    {
        var buf = new byte[8192];
        int n = 0;
        while (n < buf.Length)
        {
            int r = s.Read(buf, n, buf.Length - n);
            if (r <= 0) return null;
            n += r;
            var text = Encoding.ASCII.GetString(buf, 0, n);
            int end = text.IndexOf("\r\n\r\n", StringComparison.Ordinal);
            if (end >= 0) return Encoding.UTF8.GetString(buf, 0, end);
        }
        return null;
    }

    static Dictionary<string, string> ParseQuery(string q)
    {
        var d = new Dictionary<string, string>();
        foreach (var kv in q.Split(new[] { '&' }, StringSplitOptions.RemoveEmptyEntries))
        {
            var i = kv.IndexOf('=');
            if (i < 0) continue;
            d[Uri.UnescapeDataString(kv.Substring(0, i))] = Uri.UnescapeDataString(kv.Substring(i + 1).Replace('+', ' '));
        }
        return d;
    }

    static byte[] Json(string s) { return Encoding.UTF8.GetBytes(s); }

    static string Q(string s)
    {
        var sb = new StringBuilder("\"");
        foreach (var ch in s)
        {
            if (ch == '"' || ch == '\\') sb.Append('\\').Append(ch);
            else if (ch < 0x20) sb.Append("\\u").Append(((int)ch).ToString("x4"));
            else sb.Append(ch);
        }
        return sb.Append('"').ToString();
    }

    static void Send(NetworkStream s, int code, string type, byte[] body, bool keepAlive = true)
    {
        var status = code == 200 ? "OK" : code == 401 ? "Unauthorized" : code == 404 ? "Not Found" : "Bad Request";
        var conn = keepAlive ? "keep-alive" : "close";
        var head = "HTTP/1.1 " + code + " " + status + "\r\nContent-Type: " + type +
                   "\r\nContent-Length: " + body.Length + "\r\nConnection: " + conn + "\r\n\r\n";
        var h = Encoding.ASCII.GetBytes(head);
        s.Write(h, 0, h.Length);
        s.Write(body, 0, body.Length);
    }
}

// Phone sends "PULT?" as a UDP broadcast; every host answers with its name and port.
static class Discovery
{
    public static void Start()
    {
        var t = new Thread(delegate()
        {
            UdpClient u;
            try
            {
                u = new UdpClient(new IPEndPoint(IPAddress.Any, Program.UdpPort));
            }
            catch { return; }
            while (true)
            {
                try
                {
                    IPEndPoint from = null;
                    var data = u.Receive(ref from);
                    if (Encoding.ASCII.GetString(data) != "PULT?") continue;
                    var reply = Encoding.UTF8.GetBytes("PULT!" + Program.HttpPort + "|" + Environment.MachineName);
                    u.Send(reply, reply.Length, from);
                }
                catch { }
            }
        });
        t.IsBackground = true;
        t.Start();
    }
}
