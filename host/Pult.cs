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

[assembly: System.Reflection.AssemblyVersion("0.1.0.0")]

static class Program
{
    public const int HttpPort = 47800;
    public const int UdpPort = 47801;

    static NotifyIcon tray;
    internal static string Pin;
    internal static readonly string DataDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Pult");

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
            tray.Icon = SystemIcons.Application;
            tray.Text = "Pult";
            tray.Visible = true;
            var menu = new ContextMenuStrip();
            menu.Items.Add("Адрес и PIN", null, delegate { ShowInfo(); });
            menu.Items.Add("Разрешить в брандмауэре", null, delegate { Firewall.RunElevated(); });
            menu.Items.Add("Новый PIN", null, delegate { Pin = NewPin(); ShowInfo(); });
            menu.Items.Add(new ToolStripSeparator());
            menu.Items.Add("Выход", null, delegate { tray.Visible = false; Environment.Exit(0); });
            tray.ContextMenuStrip = menu;
            tray.MouseClick += delegate(object s, MouseEventArgs e) { if (e.Button == MouseButtons.Left) ShowInfo(); };

            if (!File.Exists(Path.Combine(DataDir, "seen"))) { File.WriteAllText(Path.Combine(DataDir, "seen"), "1"); ShowInfo(); }
            Application.Run();
        }
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

    static string NewPin()
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

    // The default network profile is often "Public", which blocks incoming connections;
    // the rules above allow the two ports on every profile.
    public static void RunElevated()
    {
        try
        {
            var psi = new ProcessStartInfo(Application.ExecutablePath, "--firewall");
            psi.Verb = "runas";
            psi.UseShellExecute = true;
            Process.Start(psi);
        }
        catch { }
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

    public static byte[] Icon(AppEntry a)
    {
        lock (icons)
        {
            byte[] png;
            if (icons.TryGetValue(a.Id, out png)) return png;
            try
            {
                using (var ico = System.Drawing.Icon.ExtractAssociatedIcon(a.Path))
                using (var src = ico.ToBitmap())
                using (var bmp = new Bitmap(96, 96))
                using (var g = Graphics.FromImage(bmp))
                {
                    g.InterpolationMode = System.Drawing.Drawing2D.InterpolationMode.HighQualityBicubic;
                    g.DrawImage(src, 0, 0, 96, 96);
                    using (var ms = new MemoryStream()) { bmp.Save(ms, ImageFormat.Png); png = ms.ToArray(); }
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

static class Keys
{
    [DllImport("user32.dll")] static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);

    static readonly Dictionary<string, byte> map = new Dictionary<string, byte> {
        { "playpause", 0xB3 }, { "next", 0xB0 }, { "prev", 0xB1 }, { "stop", 0xB2 },
        { "volup", 0xAF }, { "voldown", 0xAE }, { "mute", 0xAD } };

    public static bool Press(string name)
    {
        byte vk;
        if (!map.TryGetValue(name, out vk)) return false;
        keybd_event(vk, 0, 1, UIntPtr.Zero);  // KEYEVENTF_EXTENDEDKEY
        keybd_event(vk, 0, 3, UIntPtr.Zero);  // + KEYEVENTF_KEYUP
        return true;
    }
}

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
                c.ReceiveTimeout = 5000;
                c.SendTimeout = 5000;
                var s = c.GetStream();
                var head = ReadHead(s);
                if (head == null) return;
                var lines = head.Split(new[] { "\r\n" }, StringSplitOptions.None);
                var parts = lines[0].Split(' ');
                if (parts.Length < 2) return;
                var method = parts[0];
                var target = parts[1];
                var q = target.IndexOf('?');
                var path = q < 0 ? target : target.Substring(0, q);
                var query = ParseQuery(q < 0 ? "" : target.Substring(q + 1));
                string pin = null;
                foreach (var h in lines.Skip(1))
                    if (h.StartsWith("X-Pin:", StringComparison.OrdinalIgnoreCase)) pin = h.Substring(6).Trim();
                if (pin == null) query.TryGetValue("pin", out pin);

                if (path == "/ping")
                {
                    Send(s, 200, "application/json", Json("{\"app\":\"pult\",\"name\":" + Q(Environment.MachineName) + ",\"port\":" + Program.HttpPort + "}"));
                    return;
                }
                if (!Authorized(pin)) { Send(s, 401, "application/json", Json("{\"error\":\"pin\"}")); return; }

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
                    Send(s, 200, "application/json", Json(sb.ToString()));
                }
                else if (path == "/icon")
                {
                    string id; query.TryGetValue("id", out id);
                    var a = Apps.Find(id ?? "");
                    var png = a == null ? new byte[0] : Apps.Icon(a);
                    if (png.Length == 0) Send(s, 404, "text/plain", new byte[0]);
                    else Send(s, 200, "image/png", png);
                }
                else if (path == "/launch" && method == "POST")
                {
                    string id; query.TryGetValue("id", out id);
                    var a = Apps.Find(id ?? "");
                    if (a == null) { Send(s, 404, "application/json", Json("{\"error\":\"no app\"}")); return; }
                    Apps.Launch(a);
                    Send(s, 200, "application/json", Json("{\"ok\":true}"));
                }
                else if (path == "/key" && method == "POST")
                {
                    string name; query.TryGetValue("name", out name);
                    var ok = Keys.Press(name ?? "");
                    Send(s, ok ? 200 : 400, "application/json", Json("{\"ok\":" + (ok ? "true" : "false") + "}"));
                }
                else Send(s, 404, "application/json", Json("{\"error\":\"not found\"}"));
            }
            catch { }
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

    static void Send(NetworkStream s, int code, string type, byte[] body)
    {
        var status = code == 200 ? "OK" : code == 401 ? "Unauthorized" : code == 404 ? "Not Found" : "Bad Request";
        var head = "HTTP/1.1 " + code + " " + status + "\r\nContent-Type: " + type +
                   "\r\nContent-Length: " + body.Length + "\r\nConnection: close\r\n\r\n";
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
