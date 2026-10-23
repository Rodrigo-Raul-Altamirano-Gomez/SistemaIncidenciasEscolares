# Solo fixture de ensayo: base recién precargada, cuatro estudiantes ficticios y propietario técnico.
[CmdletBinding()]
param([string]$Origin='https://localhost:8450', [Parameter(Mandatory)][string]$ApplicationJar)
$ErrorActionPreference='Stop'
if($Origin -notmatch '^https://localhost:[0-9]+$' -or $env:DB_URL -notmatch '/i01_(auditfix|[a-zA-Z0-9_]+_e2e)$') { throw 'Usar HTTPS localhost y una base ficticia i01_auditfix o i01_*_e2e.' }
if(-not $env:I01_PASSWORD) { throw 'Falta la contraseña ficticia I01_PASSWORD.' }
$headers=@{'X-I01-Client'='true';Origin=$Origin}
$session=Invoke-RestMethod "$Origin/api/acceso/login" -Method Post -ContentType application/json -Body (@{usuario='auxiliar';clave=$env:I01_PASSWORD}|ConvertTo-Json) -Headers $headers -SessionVariable browser -SkipCertificateCheck
$headers['X-CSRF-Token']=$session.csrf
$students=Invoke-RestMethod "$Origin/api/estudiantes/buscar" -Method Post -ContentType application/json -Body '{"texto":"FIC-004"}' -Headers $headers -WebSession $browser -SkipCertificateCheck
if(@($students).Count -ne 1 -or $students[0].nombre -ne 'Estudiante Histórico Ficticio') { throw 'El fixture exige la precarga ficticia intacta FIC-004.' }
$entry=@{anio=2024;fechaHecho='2024-10-01';aproximada=$false;categoriaId='POR_CLASIFICAR';descripcion='Remanente ficticio de auditoría';participantes=@(@{estudianteId=$students[0].id;papel='DESCONOCIDO'})}
$note=Invoke-RestMethod "$Origin/api/notas" -Method Post -ContentType application/json -Body (@{solicitudId=[guid]::NewGuid();entrada=$entry}|ConvertTo-Json -Depth 8) -Headers $headers -WebSession $browser -SkipCertificateCheck
$note.cuerpo.entrada.participantes=@(); $note.cuerpo.participantes=@()
$file=Join-Path ([IO.Path]::GetTempPath()) ('i01-ficticio-'+[guid]::NewGuid().ToString('N')+'.json')
try {
    @{estudianteId=$students[0].id;sustituciones=@(@{notaId=$note.id;versionEsperada=$note.version;cuerpo=$note.cuerpo;rectificaciones=@{}})}|ConvertTo-Json -Depth 12|Set-Content -LiteralPath $file
    & java -jar $ApplicationJar --spring.main.web-application-type=none --i01.command=dispose --i01.authorization=ENSAYO-REMANENTE-I01 "--i01.file=$file" | Out-Null
    if($LASTEXITCODE -ne 0) { throw 'No se confirmó la disposición ficticia.' }
    # UUID de fixture, nunca credenciales técnicas ni relatos privados.
    Write-Output $note.id
} finally { Remove-Item -LiteralPath $file -Force }
