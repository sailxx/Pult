$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$dir = (Resolve-Path "assets\screenshots").Path

$screens = @(
    @{ Name = "screenshot-dock"; W = 1280; H = 720 },
    @{ Name = "pult-hero-dock"; W = 1280; H = 720 },
    @{ Name = "screenshot-touchpad"; W = 1280; H = 720 },
    @{ Name = "screenshot-clipboard"; W = 1280; H = 720 },
    @{ Name = "screenshot-signature"; W = 1280; H = 720 },
    @{ Name = "screenshot-capture"; W = 1280; H = 720 },
    @{ Name = "windows-tray-companion"; W = 320; H = 330 }
)

foreach ($item in $screens) {
    $s = $item.Name
    $svg = "$dir\$s.svg"
    $png = "$dir\$s.png"
    $w = $item.W
    $h = $item.H
    Start-Process -FilePath $chrome -ArgumentList "--headless --disable-gpu --screenshot=`"$png`" --window-size=$w,$h --hide-scrollbars `"file:///$svg`"" -Wait
    $len = (Get-Item $png).Length
    Write-Output "Done: $s.png ($len bytes)"
}
