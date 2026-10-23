[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$PgBin,
    [Parameter(Mandatory)][string]$BackupPrefix,
    [Parameter(Mandatory)][string]$RestoreDatabase,
    [Parameter(Mandatory)][string]$ApplicationJar,
    [Parameter(Mandatory)][string]$FrontendDist,
    [Parameter(Mandatory)][string]$TlsKeystore,
    [string]$Database='i01'
)
$ErrorActionPreference='Stop'
function Query([string]$db,[string]$sql) {
    $result=& (Join-Path $PgBin 'psql.exe') -X -A -t -d $db -v ON_ERROR_STOP=1 -c $sql
    if($LASTEXITCODE -ne 0) { throw 'Falló la comprobación de PostgreSQL.' }
    return ($result -join "`n").Trim()
}
$common=@{PgBin=$PgBin;Database=$Database;Authorization='ENSAYO-FICTICIO-I01';ApplicationJar=$ApplicationJar;FrontendDist=$FrontendDist;TlsKeystore=$TlsKeystore}
# El ensayo se ejecuta con el servidor detenido para tener un corte estable.
Query $Database "WITH target AS (UPDATE note SET blocked=true WHERE id=(SELECT id FROM note ORDER BY created_at LIMIT 1) RETURNING id) INSERT INTO audit_event(operation,result,resource) SELECT 'BLOQUEO_ENSAYO','OK',id FROM target" | Out-Null
$first="$BackupPrefix-anterior.i01b"; $latest="$BackupPrefix-actual.i01b"
& "$PSScriptRoot/backup.ps1" -Action Backup -BackupPath $first @common
& "$PSScriptRoot/backup.ps1" -Action Backup -BackupPath $latest @common
& "$PSScriptRoot/backup.ps1" -Action Verify -BackupPath $latest -Authorization ENSAYO-FICTICIO-I01
$correctKey=$env:BACKUP_KEY; $wrongKeyRejected=$false
try {
    $env:BACKUP_KEY=[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    try { & "$PSScriptRoot/backup.ps1" -Action Verify -BackupPath $latest -Authorization ENSAYO-FICTICIO-I01 } catch { $wrongKeyRejected=$true }
} finally { $env:BACKUP_KEY=$correctKey }
if(-not $wrongKeyRejected) { throw 'Una clave distinta permitió abrir el respaldo.' }
& "$PSScriptRoot/backup.ps1" -Action Verify -BackupPath $latest -Authorization ENSAYO-FICTICIO-I01
$tampered="$BackupPrefix-corrupta.i01b"; $bytes=[IO.File]::ReadAllBytes([IO.Path]::GetFullPath($latest)); $bytes[$bytes.Length-1]=$bytes[$bytes.Length-1] -bxor 1; [IO.File]::WriteAllBytes([IO.Path]::GetFullPath($tampered),$bytes)
$rejected=$false
try { & "$PSScriptRoot/backup.ps1" -Action Verify -BackupPath $tampered -Authorization ENSAYO-FICTICIO-I01 } catch { $rejected=$true }
if(-not $rejected) { throw 'No se rechazó una copia alterada.' }
& "$PSScriptRoot/backup.ps1" -Action Restore -BackupPath $latest -RestoreDatabase $RestoreDatabase -Authorization ENSAYO-FICTICIO-I01 -PgBin $PgBin
$tables=@('app_user','user_role','student','enrollment','school_year','category','note','note_participant','note_subject','note_revision','request_receipt')
$results=@()
foreach($table in $tables) {
    # Solo huellas y cantidades; no se extraen nombres, relatos o contraseñas.
    $sql="SELECT count(*)::text || ':' || md5(coalesce(string_agg(row_to_json(t)::text,'|' ORDER BY row_to_json(t)::text),'')) FROM $table t"
    $source=Query $Database $sql; $restored=Query $RestoreDatabase $sql
    if($source -ne $restored) { throw "No coincide la tabla $table." }
    $results+=@{table=$table;count=[int]($source.Split(':')[0]);fingerprint=$source.Split(':')[1];equal=$true}
}
$sessions=[int](Query $RestoreDatabase 'SELECT count(*) FROM auth_session')
$blocked=[int](Query $RestoreDatabase 'SELECT count(*) FROM note WHERE blocked')
if($sessions -ne 0 -or $blocked -lt 1) { throw 'No se conservaron las restricciones o se recuperaron sesiones antiguas.' }
$summary=@{at=[DateTimeOffset]::UtcNow.ToString('o');result='OK';database=$RestoreDatabase;tables=$results;sessionsRestored=$sessions;blockedNotes=$blocked;tamperingRejected=$rejected;wrongKeyRejected=$wrongKeyRejected;priorBackupExists=(Test-Path -LiteralPath $first);ordinaryServiceOpened=$false}
$summary | ConvertTo-Json -Depth 6 | Set-Content "$BackupPrefix-results.json"
$summary | ConvertTo-Json -Depth 6
