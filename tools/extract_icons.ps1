Add-Type -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public class HighResExtractor {
    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    public static extern uint PrivateExtractIcons(string lpszFile, int nIconIndex, int cxIcon, int cyIcon, IntPtr[] phicon, uint[] piconid, uint nIcons, uint flags);

    [DllImport("user32.dll")]
    public static extern bool DestroyIcon(IntPtr hIcon);

    public static bool Extract(string exePath, string outPath, int size = 256) {
        IntPtr[] phicon = new IntPtr[1];
        uint[] piconid = new uint[1];
        uint count = PrivateExtractIcons(exePath, 0, size, size, phicon, piconid, 1, 0);
        if (count > 0 && phicon[0] != IntPtr.Zero) {
            using (var icon = Icon.FromHandle(phicon[0])) {
                using (var bmp = icon.ToBitmap()) {
                    bmp.Save(outPath, ImageFormat.Png);
                }
            }
            DestroyIcon(phicon[0]);
            return true;
        }
        return false;
    }
}
"@ -ReferencedAssemblies System.Drawing

$apps = @{
    "vscode.png" = "C:\Users\Vlad\AppData\Local\Programs\Microsoft VS Code\Code.exe"
    "brave.png" = "C:\Program Files\BraveSoftware\Brave-Browser\Application\brave.exe"
    "chrome.png" = "C:\Program Files\Google\Chrome\Application\chrome.exe"
    "telegram.png" = "C:\Users\Vlad\AppData\Roaming\Telegram Desktop\Telegram.exe"
    "vlc.png" = "C:\Program Files\VideoLAN\VLC\vlc.exe"
    "explorer.png" = "C:\Windows\explorer.exe"
    "mpv.png" = "C:\Program Files\mpv\mpv.exe"
}

foreach ($item in $apps.GetEnumerator()) {
    $out = "assets\app-icons\" + $item.Key
    if (Test-Path $item.Value) {
        $ok = [HighResExtractor]::Extract($item.Value, $out, 256)
        Write-Output ($item.Key + " from " + $item.Value + " -> " + $ok)
    } else {
        Write-Output ("NotFound: " + $item.Value)
    }
}
