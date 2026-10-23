# Regresiones del ensayo; credenciales de propietario sobre la misma base exclusivamente ficticia.
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$PgBin,
    [Parameter(Mandatory)][ValidatePattern('^i01_[a-zA-Z0-9_]+_(test|e2e)$')][string]$Database,
    [Parameter(Mandatory)][string]$BackupPrefix,
    [Parameter(Mandatory)][string]$ApplicationJar,
    [Parameter(Mandatory)][string]$FrontendDist,
    [Parameter(Mandatory)][string]$TlsKeystore,
    [Parameter(Mandatory)][string]$ValidBackup
)
$ErrorActionPreference='Stop'
$common=@{PgBin=$PgBin;Database=$Database;BackupPrefix=$BackupPrefix;RestoreDatabase='i01_restore_safety_unused';ApplicationJar=$ApplicationJar;FrontendDist=$FrontendDist;TlsKeystore=$TlsKeystore}
$cases=@()
function Must-Fail([string]$name,[scriptblock]$operation) {
    $rejected=$false
    try { & $operation | Out-Null } catch { $rejected=$true }
    if(-not $rejected) { throw "No se rechazó: $name" }
    $script:cases+=@{name=$name;rejected=$true}
}
Must-Fail 'Base operativa rechazada antes de conectar' { $caseArgs=$common.Clone();$caseArgs.Database='i01'; & "$PSScriptRoot/test-backup.ps1" @caseArgs -FictitiousData }
Must-Fail 'Falta declaración de datos ficticios' { & "$PSScriptRoot/test-backup.ps1" @common }
Must-Fail 'Fallo después de pg_dump no altera datos de origen' { $caseArgs=$common.Clone();$caseArgs.ApplicationJar="$BackupPrefix-no-existe.jar"; & "$PSScriptRoot/test-backup.ps1" @caseArgs -FictitiousData }
$failure=Get-Content "$BackupPrefix-results.json" -Raw | ConvertFrom-Json
$backupState=Get-Content "$BackupPrefix-anterior.i01b.Backup.status.json" -Raw | ConvertFrom-Json
if($failure.result -ne 'FALLO' -or -not $failure.sourceBusinessUnchanged -or $backupState.result -ne 'FALLO') { throw 'El fallo intermedio no dejó un resultado verificable y origen íntegro.' }
$guard="$BackupPrefix-guard.i01b"
Copy-Item -LiteralPath $ValidBackup -Destination $guard
$correctKey=$env:BACKUP_KEY
try {
    $env:BACKUP_KEY='formato-invalido'
    Must-Fail 'Clave malformada registra FALLO sin dejar OK anterior' { & "$PSScriptRoot/backup.ps1" -Action Verify -BackupPath $guard -Authorization ENSAYO-SAFETY-I01 }
} finally { $env:BACKUP_KEY=$correctKey }
$invalidKeyState=Get-Content "$guard.Verify.status.json" -Raw | ConvertFrom-Json
if($invalidKeyState.result -ne 'FALLO') { throw 'La clave malformada no dejó estado FALLO.' }
New-Item -ItemType Directory -Path "$guard.recovered" | Out-Null
$target='i01_restore_guard_'+[guid]::NewGuid().ToString('N')
Must-Fail 'Directorio de recuperación previo no crea base' { & "$PSScriptRoot/backup.ps1" -Action Restore -BackupPath $guard -RestoreDatabase $target -Authorization ENSAYO-SAFETY-I01 -PgBin $PgBin }
$count=& (Join-Path $PgBin 'psql.exe') -X -A -t -d postgres -v ON_ERROR_STOP=1 -c "SELECT count(*) FROM pg_database WHERE datname='$target'"
if($LASTEXITCODE -ne 0 -or [int]$count -ne 0) { throw 'Se creó una base antes de comprobar el directorio recuperado.' }
$summary=@{result='OK';cases=$cases;sourceBusinessUnchanged=$failure.sourceBusinessUnchanged;intermediateBackupStatus=$backupState.result;invalidKeyStatus=$invalidKeyState.result;restoreDatabaseCreated=$false}
$summary | ConvertTo-Json -Depth 8 | Set-Content "$BackupPrefix-safety-results.json"
$summary | ConvertTo-Json -Depth 8
