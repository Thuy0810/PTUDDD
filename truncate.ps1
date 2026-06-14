$content = Get-Content "e:\Hoc Tap\PTUDDD\app\src\main\res\values\strings.xml" -Encoding UTF8
$content = $content | Select-Object -First 239
Add-Content -Path "e:\Hoc Tap\PTUDDD\app\src\main\res\values\strings_new.xml" -Value $content -Encoding UTF8 -NoNewline
