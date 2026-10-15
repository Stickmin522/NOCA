$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskSource=Join-Path $taskRoot 'noca'
$taskBuild=Join-Path $env:TEMP 'noca-build'
New-Item -ItemType Directory -Path $taskBuild -Force | Out-Null
foreach($taskItem in Get-ChildItem -LiteralPath $taskSource -Force){
    if($taskItem.Name -notin @('.git','.gradle','build','signing')){
        Copy-Item -LiteralPath $taskItem.FullName -Destination $taskBuild -Recurse -Force
    }
}
$env:GRADLE_USER_HOME=Join-Path $taskRoot 'work\noca-gradle'
$env:GRADLE_RO_DEP_CACHE='C:\Users\User\.gradle\caches'
$env:NOCA_CARGO=Join-Path $taskRoot 'work\noca-tooling\rust\bin\cargo.exe'
$env:ANDROID_USER_HOME=Join-Path $taskRoot 'work\noca-android'
$env:JAVA_HOME='D:\Program Files\Android Studio\jbr'
$env:NOCA_MAVEN_MIRROR=Join-Path $taskRoot 'work\maven'
$taskJava=Join-Path $env:JAVA_HOME 'bin\java.exe'
$taskGradle='C:\Users\User\.gradle\wrapper\dists\gradle-9.5.0-bin\bvnork1r7n8i6kp5cnkibsc9q\gradle-9.5.0\lib\gradle-gradle-cli-main-9.5.0.jar'
& $taskJava '-Xmx3072m' '-Dfile.encoding=UTF-8' '-Dorg.gradle.daemon=false' '-Dorg.gradle.internal.instrumentation.agent=false' -jar $taskGradle -p $taskBuild :app:assembleDebug --no-daemon '-Dorg.gradle.jvmargs=-Xmx3072m -Dfile.encoding=UTF-8' --max-workers=1 --console=plain '-Dorg.gradle.internal.http.connectionTimeout=10000' '-Dorg.gradle.internal.http.socketTimeout=10000'
$taskExit=$LASTEXITCODE
$taskOutput=Join-Path $taskRoot 'outputs'
New-Item -ItemType Directory -Path $taskOutput -Force | Out-Null
if(Test-Path -LiteralPath (Join-Path $taskBuild 'app\build\outputs\apk\debug\app-debug.apk')){Copy-Item -LiteralPath (Join-Path $taskBuild 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $taskOutput 'NOCA-arm64-debug.apk') -Force}
Write-Output ('Build exit: '+$taskExit)
exit $taskExit
