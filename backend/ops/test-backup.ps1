[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$PgBin,
    [Parameter(Mandatory)][string]$BackupPrefix,
    [Parameter(Mandatory)][string]$RestoreDatabase,
    [Parameter(Mandatory)][string]$ApplicationJar,
    [Parameter(Mandatory)][string]$FrontendDist,
    [Parameter(Mandatory)][string]$TlsKeystore,
    [Parameter(Mandatory)][ValidatePattern('^i01_[a-zA-Z0-9_]+_(test|e2e)$')][string]$Database,
    [switch]$FictitiousData
)
$ErrorActionPreference='Stop'
if(-not $FictitiousData) { throw 'El ensayo exige -FictitiousData y una base aislada i01_*_test o i01_*_e2e; nunca datos operativos.' }
if($RestoreDatabase -eq $Database) { throw 'Origen y destino deben ser diferentes.' }
function Query([string]$db,[string]$sql) {
    $result=& (Join-Path $PgBin 'psql.exe') -X -A -t -d $db -v ON_ERROR_STOP=1 -c $sql
    if($LASTEXITCODE -ne 0) { throw 'Falló la comprobación de PostgreSQL.' }
    return ($result -join "`n").Trim()
}
$common=@{PgBin=$PgBin;Database=$Database;Authorization='ENSAYO-FICTICIO-I01';ApplicationJar=$ApplicationJar;FrontendDist=$FrontendDist;TlsKeystore=$TlsKeystore}
# El servicio debe estar detenido; el fixture ya debe incluir una restricción, no se modifica aquí.
$tables=@('app_user','user_role','student','enrollment','school_year','category','note','note_participant','note_subject','note_revision','request_receipt')
function Snapshot([string]$table) {
    return Query $Database "SELECT count(*)::text || ':' || md5(coalesce(string_agg(row_to_json(t)::text,'|' ORDER BY row_to_json(t)::text),'')) FROM $table t"
}
$sourceBefore=@{}; foreach($table in ($tables+@('auth_session','login_throttle'))) { $sourceBefore[$table]=Snapshot $table }
if([int](Query $Database 'SELECT count(*) FROM note WHERE blocked') -lt 1) { throw 'Preparar previamente un aporte ficticio bloqueado en la base aislada; el ensayo no lo modifica.' }
$summary=@{at=[DateTimeOffset]::UtcNow.ToString('o');result='FALLO';sourceBusinessUnchanged=$null;ordinaryServiceOpened=$false}
try {
$first="$BackupPrefix-anterior.i01b"; $latest="$BackupPrefix-actual.i01b"
& "$PSScriptRoot/backup.ps1" -Action Backup -BackupPath $first @common
$beforeLatest=[long](Query $Database 'SELECT coalesce(max(id),0) FROM audit_event')
& "$PSScriptRoot/backup.ps1" -Action Backup -BackupPath $latest @common
# Con el servicio detenido, esta copia añade INICIADO antes de pg_dump y OK después.
# El corte incluye INICIADO; OK todavía no pertenece al contenido del respaldo.
$copyEvents=@((Query $Database "SELECT coalesce(json_agg(t ORDER BY id),'[]') FROM (SELECT id,operation,result FROM audit_event WHERE id>$beforeLatest) t" | ConvertFrom-Json))
if($copyEvents.Count -ne 2 -or $copyEvents[0].operation -ne 'BACKUP' -or $copyEvents[0].result -ne 'INICIADO' -or $copyEvents[1].operation -ne 'BACKUP' -or $copyEvents[1].result -ne 'OK') { throw 'El corte de auditoría no es estable: detener las operaciones concurrentes y repetir con otros destinos.' }
$auditCut=[long]$copyEvents[0].id
$auditSql="SELECT count(*)::text || ':' || encode(sha256(convert_to(coalesce(string_agg(row_to_json(t)::text,'|' ORDER BY t.id),''),'UTF8')),'hex') FROM audit_event t WHERE id<=$auditCut"
$auditExpected=Query $Database $auditSql
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
$results=@()
foreach($table in $tables) {
    # Solo huellas y cantidades; no se extraen nombres, relatos o contraseñas.
    $sql="SELECT count(*)::text || ':' || md5(coalesce(string_agg(row_to_json(t)::text,'|' ORDER BY row_to_json(t)::text),'')) FROM $table t"
    $source=$sourceBefore[$table]; $restored=Query $RestoreDatabase $sql
    if($source -ne $restored) { throw "No coincide la tabla $table." }
    $results+=@{table=$table;count=[int]($source.Split(':')[0]);fingerprint=$source.Split(':')[1];equal=$true}
}
function Assert-AuditEqual([string]$expected,[string]$actual) {
    if($expected -ne $actual) { throw 'No coincide la evidencia de auditoría incluida en el respaldo.' }
}
$auditRestored=Query $RestoreDatabase $auditSql
Assert-AuditEqual $auditExpected $auditRestored
$restoredLater=@((Query $RestoreDatabase "SELECT coalesce(json_agg(t ORDER BY id),'[]') FROM (SELECT id,operation,result FROM audit_event WHERE id>$auditCut) t" | ConvertFrom-Json))
if($restoredLater.Count -ne 1 -or $restoredLater[0].operation -ne 'RESTAURACION' -or $restoredLater[0].result -ne 'AISLADA') { throw 'Hay eventos inesperados posteriores al corte en la base restaurada.' }
# Pruebas negativas sin alterar evidencia protegida: simular omisión y cambio en la lectura.
$auditNegatives=@()
foreach($simulation in @('omission','alteration')) {
    $row=if($simulation -eq 'alteration') { "(to_jsonb(t) || jsonb_build_object('result','ALTERADO_EN_ENSAYO'))::text" } else { 'row_to_json(t)::text' }
    $filter=if($simulation -eq 'omission') { ' AND id<>(SELECT min(id) FROM audit_event)' } else { '' }
    $bad=Query $RestoreDatabase "SELECT count(*)::text || ':' || encode(sha256(convert_to(coalesce(string_agg($row,'|' ORDER BY t.id),''),'UTF8')),'hex') FROM audit_event t WHERE id<=$auditCut$filter"
    $detected=$false
    try { Assert-AuditEqual $auditExpected $bad } catch { $detected=$true }
    if(-not $detected) { throw "No se detectó la simulación de $simulation en la auditoría." }
    $auditNegatives+=@{simulation=$simulation;rejected=$detected}
}
$auditResult=@{cutId=$auditCut;includedCount=[int]$auditExpected.Split(':')[0];sha256=$auditExpected.Split(':')[1];equal=$true;sourceAfterCut=$copyEvents[1];restoredAfterCut=$restoredLater;negativeChecks=$auditNegatives}
$sessions=[int](Query $RestoreDatabase 'SELECT count(*) FROM auth_session')
$blocked=[int](Query $RestoreDatabase 'SELECT count(*) FROM note WHERE blocked')
if($sessions -ne 0 -or $blocked -lt 1) { throw 'No se conservaron las restricciones o se recuperaron sesiones antiguas.' }
$summary=@{at=[DateTimeOffset]::UtcNow.ToString('o');result='OK';database=$RestoreDatabase;tables=$results;audit=$auditResult;sessionsRestored=$sessions;blockedNotes=$blocked;tamperingRejected=$rejected;wrongKeyRejected=$wrongKeyRejected;priorBackupExists=(Test-Path -LiteralPath $first);ordinaryServiceOpened=$false}
} finally {
    # También tras fallos intermedios: no hay UPDATE/DELETE sobre datos de negocio de origen.
    $same=$true
    try { foreach($table in $sourceBefore.Keys) { if((Snapshot $table) -ne $sourceBefore[$table]) { $same=$false } } }
    catch { $same=$false; $summary.sourceVerification='NO_VERIFICABLE' }
    $summary.sourceBusinessUnchanged=$same
    if(-not $same) { $summary.result='FALLO' }
    $summary | ConvertTo-Json -Depth 8 | Set-Content "$BackupPrefix-results.json"
    if(-not $same) { throw 'La integridad del origen no se pudo confirmar; revisar el informe de ensayo.' }
}
$summary | ConvertTo-Json -Depth 8
