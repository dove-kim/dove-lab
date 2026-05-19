# Windows 전용. Linux / macOS 는 ./gradlew 를 그대로 사용한다.
#
# gradle.properties 의 org.gradle.java.home 을 JAVA_HOME 으로 설정한 뒤 gradlew.bat 을 실행한다.
# 사용: .\gw.ps1 clean build
#       .\gw.ps1 :scheduler:bootRun

$propsFile = Join-Path $PSScriptRoot "gradle.properties"

if (Test-Path $propsFile) {
    $line = Get-Content $propsFile | Where-Object { $_ -match "^\s*org\.gradle\.java\.home\s*=\s*(.+)" }
    if ($line) {
        $env:JAVA_HOME = $line -replace "^\s*org\.gradle\.java\.home\s*=\s*", ""
    }
}

$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
& "$PSScriptRoot\gradlew.bat" @args
