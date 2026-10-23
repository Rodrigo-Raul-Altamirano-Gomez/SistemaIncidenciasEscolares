# PowerShell 7. Solo operador técnico con autorización externa y credenciales de propietario.
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet('Backup','Restore','Verify','Status')][string]$Action,
    [Parameter(Mandatory)][string]$BackupPath,
    [string]$PgBin = '', [string]$Database = 'i01', [string]$RestoreDatabase = '',
    [string]$Authorization = '', [string]$ApplicationJar = '', [string]$FrontendDist = '', [string]$TlsKeystore = ''
)
$ErrorActionPreference = 'Stop'
$backupFile = [IO.Path]::GetFullPath($BackupPath)
$statusFile = "$backupFile.$Action.status.json"
if ($Action -eq 'Status') { foreach($kind in @('Backup','Restore','Verify')) { $entry="$backupFile.$kind.status.json"; if(Test-Path -LiteralPath $entry) { Get-Content -LiteralPath $entry } }; exit 0 }
if ([string]::IsNullOrWhiteSpace($Authorization) -or $Authorization.Length -gt 120) { throw 'Indica una referencia externa de autorización (sin datos personales).' }
$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$staging = Join-Path $tempRoot ('i01-' + [Guid]::NewGuid().ToString('N'))
$started = [DateTimeOffset]::UtcNow
$status = [ordered]@{ action=$Action; at=$started.ToString('o'); authorization=$Authorization; result='FALLO'; seconds=0; sha256=''; detail='No confirmado' }
$key=$null; $plain=$null; $aes=$null
function Pg([string]$exe, [string[]]$Arguments) {
    & (Join-Path $PgBin "$exe.exe") @Arguments 2> (Join-Path $staging 'pg-error.txt')
    if ($LASTEXITCODE -ne 0) { throw "Falló $exe. El resultado no está confirmado; revisa disponibilidad/credenciales y contacta al responsable operativo." }
}
try {
    $key = [Convert]::FromBase64String($env:BACKUP_KEY)
    if ($key.Length -ne 32) { throw 'BACKUP_KEY debe contener 32 bytes Base64; custodiar fuera del respaldo.' }
    New-Item -ItemType Directory -Path $staging | Out-Null
    # Antes de cualquier dato descifrado: no heredar permisos de otros usuarios del puesto.
    & icacls.exe $staging /inheritance:r /grant:r "$($env:USERNAME):(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo proteger el directorio temporal.' }
    if ($Action -eq 'Backup') {
        if ([Convert]::FromBase64String($env:DATA_KEY).Length -ne 32) { throw 'Falta la clave válida de los datos; no se generará una copia incompleta.' }
        if (Test-Path -LiteralPath $backupFile) { throw 'El destino ya existe. Usa otro nombre para conservar la copia anterior.' }
        $payload = New-Item -ItemType Directory -Path (Join-Path $staging 'payload')
        Pg 'psql' @('-d',$Database,'-v','ON_ERROR_STOP=1','-c',"INSERT INTO audit_event(operation,result,metadata) VALUES ('BACKUP','INICIADO',jsonb_build_object('tecnico',current_user));") | Out-Null
        Pg 'pg_dump' @('-d',$Database,'-Fc','-f',(Join-Path $payload 'database.dump'))
        Copy-Item -LiteralPath $ApplicationJar -Destination (Join-Path $payload 'application.jar')
        Copy-Item -LiteralPath $FrontendDist -Destination (Join-Path $payload 'frontend') -Recurse
        if ($TlsKeystore) { Copy-Item -LiteralPath $TlsKeystore -Destination (Join-Path $payload 'tls.p12') }
        @{ DATA_KEY=$env:DATA_KEY; APP_ORIGIN=$env:APP_ORIGIN; COOKIE_SECURE=$env:COOKIE_SECURE; TLS_KEYSTORE_PASSWORD=$env:SERVER_SSL_KEY_STORE_PASSWORD; timezone='America/Lima'; version='I01'; jarSha256=(Get-FileHash -LiteralPath $ApplicationJar).Hash } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $payload 'configuration.json')
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zip = Join-Path $staging 'payload.zip'
        [IO.Compression.ZipFile]::CreateFromDirectory($payload,$zip)
        $plain=[IO.File]::ReadAllBytes($zip); $nonce=[Security.Cryptography.RandomNumberGenerator]::GetBytes(12)
        $encrypted=[byte[]]::new($plain.Length); $tag=[byte[]]::new(16)
        $aes=[Security.Cryptography.AesGcm]::new($key,16)
        $aes.Encrypt($nonce,$plain,$encrypted,$tag,[Text.Encoding]::UTF8.GetBytes('I01-backup-v1'))
        [IO.File]::WriteAllBytes("$backupFile.pending",[byte[]]([Text.Encoding]::ASCII.GetBytes('I01B1') + $nonce + $tag + $encrypted))
        Move-Item -LiteralPath "$backupFile.pending" -Destination $backupFile
        Pg 'psql' @('-d',$Database,'-v','ON_ERROR_STOP=1','-c',"INSERT INTO audit_event(operation,result,metadata) VALUES ('BACKUP','OK',jsonb_build_object('tecnico',current_user));") | Out-Null
        $status.detail='Copia cifrada de datos, configuración y ejecutables. Conservar también una copia anterior aislada.'
    } else {
        $bytes=[IO.File]::ReadAllBytes($backupFile)
        if ($bytes.Length -lt 34 -or [Text.Encoding]::ASCII.GetString($bytes,0,5) -ne 'I01B1') { throw 'Formato de copia no admitido.' }
        $nonce=$bytes[5..16]; $tag=$bytes[17..32]; $cipher=$bytes[33..($bytes.Length-1)]; $plain=[byte[]]::new($cipher.Length)
        $aes=[Security.Cryptography.AesGcm]::new($key,16)
        $aes.Decrypt($nonce,$cipher,$tag,$plain,[Text.Encoding]::UTF8.GetBytes('I01-backup-v1'))
        if ($Action -eq 'Restore') {
            if ($RestoreDatabase -notmatch '^i01_restore_[a-zA-Z0-9_]+$') { throw 'Solo se admite una base NUEVA con prefijo i01_restore_.' }
            $recovery="$backupFile.recovered"
            if (Test-Path -LiteralPath $recovery) { throw 'El directorio recuperado ya existe; no se sobrescribe ni se crea otra base.' }
            $zip=Join-Path $staging 'payload.zip'; [IO.File]::WriteAllBytes($zip,$plain)
            $payload=Join-Path $staging 'payload'; [IO.Compression.ZipFile]::ExtractToDirectory($zip,$payload)
            $config=Get-Content -LiteralPath (Join-Path $payload 'configuration.json') -Raw | ConvertFrom-Json
            if ((Get-FileHash (Join-Path $payload 'application.jar')).Hash -ne $config.jarSha256) { throw 'El ejecutable no coincide con la configuración.' }
            Pg 'createdb' @($RestoreDatabase)
            Pg 'psql' @('-d',$RestoreDatabase,'-v','ON_ERROR_STOP=1','-c',"REVOKE CONNECT ON DATABASE $RestoreDatabase FROM PUBLIC; SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$RestoreDatabase' AND pid<>pg_backend_pid();") | Out-Null
            Pg 'pg_restore' @('--exit-on-error','--no-owner','--no-acl','-d',$RestoreDatabase,(Join-Path $payload 'database.dump'))
            Pg 'psql' @('-d',$RestoreDatabase,'-v','ON_ERROR_STOP=1','-c',"DELETE FROM auth_session; INSERT INTO audit_event(operation,result,metadata) VALUES ('RESTAURACION','AISLADA',jsonb_build_object('tecnico',current_user));") | Out-Null
            # Ejecutables y configuración recuperados quedan en un directorio protegido junto al archivo cifrado.
            New-Item -ItemType Directory -Path $recovery | Out-Null
            & icacls.exe $recovery /inheritance:r /grant:r "$($env:USERNAME):(OI)(CI)F" 'SYSTEM:(OI)(CI)F' | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo proteger la configuración recuperada.' }
            Get-ChildItem -LiteralPath $payload | Where-Object Name -ne 'database.dump' | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $recovery -Recurse }
            $status.detail='Base restaurada AISLADA y sesiones retiradas. Aplicar decisiones/restricciones posteriores y verificar integridad antes de habilitar servicio. Configuración/ejecutables en .recovered.'
        } else { $status.detail='Integridad y autenticidad AES-GCM verificadas. Esto no equivale a un ensayo de restauración.' }
    }
    $status.result='OK'; $status.sha256=(Get-FileHash -LiteralPath $backupFile -Algorithm SHA256).Hash
} catch {
    $status.detail='Fallo: no se confirma copia/restauración. Revisar medios, integridad y credenciales; contactar al responsable operativo.'
    throw
} finally {
    if ($null -ne $aes) { $aes.Dispose() }
    $status.seconds=[math]::Round(([DateTimeOffset]::UtcNow-$started).TotalSeconds,3)
    $status | ConvertTo-Json | Set-Content -LiteralPath $statusFile
    # Comprobación explícita antes del único borrado recursivo.
    $resolvedStage=[IO.Path]::GetFullPath($staging)
    if ((Test-Path -LiteralPath $resolvedStage) -and $resolvedStage.StartsWith($tempRoot,[StringComparison]::OrdinalIgnoreCase) -and [IO.Path]::GetFileName($resolvedStage) -match '^i01-[a-f0-9]{32}$') { Remove-Item -LiteralPath $resolvedStage -Recurse -Force }
    if ($null -ne $key) { [Array]::Clear($key) }; if ($null -ne $plain) { [Array]::Clear($plain) }
}
