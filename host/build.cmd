@echo off
cd /d "%~dp0"
"%WINDIR%\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /nologo /target:winexe /platform:x64 /optimize+ /debug- /win32icon:Pult.ico /out:Pult.exe /r:System.Windows.Forms.dll /r:System.Drawing.dll Pult.cs
