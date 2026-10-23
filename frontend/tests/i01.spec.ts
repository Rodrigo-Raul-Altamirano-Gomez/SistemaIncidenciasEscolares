import { test, expect } from '@playwright/test'
import type { Page } from '@playwright/test'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'
import { resolve } from 'node:path'
const password = process.env.I01_PASSWORD
if (!password) throw new Error('Define I01_PASSWORD con la contraseña ficticia de precarga.')
async function login(page: Page, user: string) {
  await page.goto('/')
  await page.getByLabel('Usuario', { exact: true }).fill(user)
  await page.getByLabel('Contraseña', { exact: true }).fill(password!)
  const firstList = page.waitForResponse(r => r.url().endsWith('/api/notas/consultar') && r.request().method() === 'POST' && r.status() === 200)
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
  await firstList
  await expect(page.getByRole('heading', { name: 'Mis registros cotidianos', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Aplicar filtros', exact: true })).toBeEnabled()
}
async function iniciar(page: Page, text: string) {
  await page.getByRole('button', { name: 'Nuevo registro', exact: true }).click()
  await page.getByLabel('Nombre o código escolar', { exact: true }).fill('  sAMIRA   torRES ')
  await page.getByRole('button', { name: 'Buscar estudiantes', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Añadir FIC-001', exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Añadir FIC-002', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Añadir FIC-001', exact: true }).click()
  await page.getByLabel('Descripción objetiva', { exact: true }).fill(text)
}
for (const role of ['docente', 'auxiliar', 'directivo']) {
  test(`${role}: registrar, recuperar en otra sesión y corregir`, async ({ page, browser }) => {
    await login(page, role)
    const text = `Ensayo E2E ${role} ${Date.now()}`
    await iniciar(page, text)
    await page.getByRole('button', { name: 'Añadir FIC-002', exact: true }).click()
    await expect(page.getByLabel('Papel de FIC-002')).toHaveValue('DESCONOCIDO')
    await page.getByLabel('Papel de FIC-002').selectOption('AFECTADO')
    await page.getByLabel('Hubo una acción realizada', { exact: true }).check()
    await page.getByLabel('Acción realizada', { exact: true }).fill('Conversación ficticia sobre el turno de participación.')
    await page.getByLabel('Hubo un compromiso acordado', { exact: true }).check()
    await page.getByLabel('Compromiso', { exact: true }).fill('Escuchar el turno en la próxima actividad.')
    await page.getByLabel('Persona que lo asume', { exact: true }).fill('Estudiante ficticio FIC-001')
    await page.getByRole('button', { name: 'Guardar registro', exact: true }).click()
    await expect(page.getByText('Registro guardado y confirmado.', { exact: true })).toBeVisible()
    await expect(page.locator('main')).toContainText('Afectado/a')
    const second = await browser.newContext({ ignoreHTTPSErrors: true })
    const other = await second.newPage()
    try {
      await login(other, role)
      await other.getByRole('listitem').filter({ hasText: text }).getByRole('button', { name: 'Ver aporte', exact: true }).click()
      await expect(other.locator('main')).toContainText('Conversación ficticia sobre el turno')
      await other.getByRole('button', { name: 'Corregir mi aporte', exact: true }).click()
      await other.getByLabel('Descripción objetiva', { exact: true }).fill(`${text} corregido`)
      await other.getByLabel('Papel de FIC-002').selectOption('TESTIGO')
      await other.getByRole('button', { name: 'Guardar corrección', exact: true }).click()
      await expect(other.locator('main')).toContainText(`Antes: ${text}`)
      await expect(other.locator('main')).toContainText(`Después: ${text} corregido`)
      await expect(other.locator('main')).toContainText('Antes: Samira Torres Luna (FIC-001)')
      await expect(other.locator('main')).toContainText('Samira Tórres Luna (FIC-002) · Testigo')
    } finally { await second.close() }
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Acceso del personal' })).toBeVisible()
    await page.goBack()
    await expect(page.getByText(text, { exact: true })).toHaveCount(0)
  })
}
test('respuesta perdida después de persistir: reintento seguro y texto inerte', async ({ page }) => {
  await login(page, 'docente')
  const text = `<img src=x onerror="window.__xss=1"> E2E inerte ${Date.now()}`
  await iniciar(page, text)
  let committed = ''
  await page.route('**/api/notas', async route => {
    const response = await route.fetch()
    const result = await response.json()
    committed = result.id
    await route.abort('failed')
  }, { times: 1 })
  await page.getByRole('button', { name: 'Guardar registro', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Comprobar guardado', exact: true })).toBeVisible()
  await expect(page.getByLabel('Descripción objetiva')).toHaveValue(text)
  await page.getByRole('button', { name: 'Comprobar guardado', exact: true }).click()
  await expect(page.getByText('Registro guardado y confirmado.', { exact: true })).toBeVisible()
  expect(committed).not.toBe('')
  expect(await page.evaluate(() => (window as unknown as { __xss?: number }).__xss)).toBeUndefined()
  await page.getByRole('button', { name: 'Volver a mis registros', exact: true }).click()
  await expect(page.getByRole('listitem').filter({ hasText: text })).toHaveCount(1)
})
test('compromiso sin acción, recuperación y retirada de acción conservan el acuerdo', async ({ page, browser }) => {
  await login(page, 'auxiliar')
  const text = `Compromiso independiente ${Date.now()}`
  await iniciar(page, text)
  await page.getByLabel('Hubo un compromiso acordado', { exact: true }).check()
  await page.getByLabel('Compromiso', { exact: true }).fill('Acuerdo ficticio sin actuación inmediata documentada.')
  await page.getByLabel('Persona que lo asume', { exact: true }).fill('Persona ficticia')
  await page.getByLabel('Hubo una acción realizada', { exact: true }).check()
  await page.getByLabel('Acción realizada', { exact: true }).fill('Acción ficticia introducida por error')
  await page.getByLabel('Hubo una acción realizada', { exact: true }).uncheck()
  await expect(page.getByLabel('Compromiso', { exact: true })).toHaveValue('Acuerdo ficticio sin actuación inmediata documentada.')
  await page.getByRole('button', { name: 'Guardar registro', exact: true }).click()
  await expect(page.locator('main')).toContainText('No se registró acción inmediata.')
  await expect(page.locator('main')).toContainText('Acuerdo ficticio sin actuación inmediata documentada.')
  const second = await browser.newContext({ ignoreHTTPSErrors: true })
  try {
    const other = await second.newPage()
    await login(other, 'auxiliar')
    await other.getByRole('listitem').filter({ hasText: text }).getByRole('button', { name: 'Ver aporte', exact: true }).click()
    await expect(other.locator('main')).toContainText('Acuerdo ficticio sin actuación inmediata documentada.')
    await other.getByRole('button', { name: 'Corregir mi aporte', exact: true }).click()
    await other.getByLabel('Compromiso', { exact: true }).fill('Acuerdo ficticio corregido sin acción.')
    await other.getByRole('button', { name: 'Guardar corrección', exact: true }).click()
    await expect(other.locator('main')).toContainText('Acuerdo ficticio corregido sin acción.')
    await expect(other.locator('main')).toContainText('Rectificaciones automáticas')
  } finally { await second.close() }
})
test('remanente autorizado se consulta y corrige sin inventar participantes', async ({ page }) => {
  // Preparar por el procedimiento técnico documentado; UUID ficticio, nunca credencial técnica en el navegador.
  const id = process.env.I01_ANON_NOTE_ID
  test.skip(!id, 'Definir I01_ANON_NOTE_ID de un remanente ficticio del auxiliar preparado con dispose.')
  await login(page, 'auxiliar')
  const session = await (await page.request.get('/api/acceso/sesion')).json()
  const response = await page.request.get(`/api/notas/${id}`, { headers: { 'X-I01-Client': 'true', 'X-CSRF-Token': session.csrf } })
  expect(response.ok()).toBe(true)
  const record = await response.json()
  expect(record.cuerpo.participantes).toHaveLength(0)
  await page.getByRole('listitem').filter({ hasText: record.cuerpo.entrada.descripcion }).getByRole('button', { name: 'Ver aporte', exact: true }).click()
  await expect(page.locator('main')).toContainText('Participaciones retiradas por disposición autorizada.')
  await page.getByRole('button', { name: 'Corregir mi aporte', exact: true }).click()
  await expect(page.locator('main')).toContainText('Puedes corregir el remanente sin inventar estudiantes.')
  await page.getByLabel('Descripción objetiva', { exact: true }).fill(`${record.cuerpo.entrada.descripcion} Revisado en E2E.`)
  await page.getByRole('button', { name: 'Guardar corrección', exact: true }).click()
  await expect(page.getByText('Registro guardado y confirmado.', { exact: true })).toBeVisible()
  await expect(page.locator('main')).toContainText('Participaciones retiradas por disposición autorizada.')
})
test('320 px, teclado, 200 %, etiquetas y aviso de abandono', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 800 })
  await login(page, 'auxiliar')
  await iniciar(page, 'Borrador ficticio accesible')
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await page.getByLabel('Descripción objetiva').focus()
  await page.keyboard.press('Tab')
  expect(await page.evaluate(() => document.activeElement?.tagName)).not.toBe('BODY')
  await page.evaluate(() => { document.documentElement.style.fontSize = '200%' })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await expect(page.getByRole('button', { name: 'Guardar registro', exact: true })).toBeEnabled()
  page.once('dialog', dialog => dialog.dismiss())
  await page.getByRole('button', { name: 'Mis registros', exact: true }).click()
  await expect(page.getByLabel('Descripción objetiva')).toHaveValue('Borrador ficticio accesible')
})
test('aviso y ocultamiento al caducar: recupera borrador solo para la misma cuenta', async ({ page }) => {
  await login(page, 'directivo')
  await iniciar(page, 'Borrador ficticio de sesión')
  // El reloj del navegador comprueba la advertencia/UI; el vencimiento del servidor se ensaya en Java/PostgreSQL.
  await page.route('**/api/acceso/sesion', route => route.abort())
  await page.clock.install()
  await page.clock.fastForward(18 * 60 * 1000 + 1000)
  await expect(page.getByRole('alert')).toContainText('Tu sesión terminará')
  await page.clock.fastForward(2 * 60 * 1000 + 1000)
  await expect(page.getByRole('heading', { name: 'Acceso del personal' })).toBeVisible()
  await expect(page.getByLabel('Descripción objetiva')).toHaveCount(0)
  await page.getByLabel('Usuario', { exact: true }).fill('directivo')
  await page.getByLabel('Contraseña', { exact: true }).fill(password!)
  await page.clock.setSystemTime(new Date())
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
  await expect(page.getByLabel('Descripción objetiva')).toHaveValue('Borrador ficticio de sesión')
  await page.clock.fastForward(20 * 60 * 1000 + 1000)
  await expect(page.getByRole('heading', { name: 'Acceso del personal' })).toBeVisible()
  await page.clock.setSystemTime(new Date())
  await page.getByLabel('Usuario', { exact: true }).fill('ajeno')
  await page.getByLabel('Contraseña', { exact: true }).fill(password!)
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Mis registros cotidianos', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Nuevo registro', exact: true }).click()
  await expect(page.getByLabel('Descripción objetiva')).toHaveValue('')
})
test('registro completo solo con teclado', async ({ page }) => {
  await login(page, 'auxiliar')
  const tabTo = async (target: ReturnType<Page['getByRole']>) => {
    for (let i = 0; i < 60; i++) {
      if (await target.evaluate(e => e === document.activeElement)) return
      await page.keyboard.press('Tab')
    }
    await expect(target).toBeFocused()
  }
  await tabTo(page.getByRole('button', { name: 'Nuevo registro', exact: true }))
  await page.keyboard.press('Enter')
  await tabTo(page.getByLabel('Nombre o código escolar', { exact: true }))
  await page.keyboard.type('FIC-003')
  await tabTo(page.getByRole('button', { name: 'Buscar estudiantes', exact: true }))
  await page.keyboard.press('Enter')
  const add = page.getByRole('button', { name: 'Añadir FIC-003', exact: true })
  await expect(add).toBeVisible()
  await tabTo(add); await page.keyboard.press('Enter')
  await tabTo(page.getByLabel('Descripción objetiva', { exact: true }))
  await page.keyboard.type('Registro ficticio completado por teclado.')
  await tabTo(page.getByRole('button', { name: 'Guardar registro', exact: true }))
  await page.keyboard.press('Enter')
  await expect(page.getByText('Registro guardado y confirmado.', { exact: true })).toBeVisible()
})
test('muestra inicial de rendimiento: veinte búsquedas y guardados desde UI', async ({ page, browser }, testInfo) => {
  test.setTimeout(120000)
  await login(page, 'docente')
  const session = await (await page.request.get('/api/acceso/sesion')).json()
  const students = await (await page.request.post('/api/estudiantes/buscar', { headers: { 'X-I01-Client': 'true', 'X-CSRF-Token': session.csrf }, data: { texto: '' } })).json() as { id: string }[]
  const studentCount = new Set(students.map(s => s.id)).size
  const searches: number[] = [], saves: number[] = []
  for (let i = 0; i < 20; i++) {
    await page.getByRole('button', { name: 'Nuevo registro', exact: true }).click()
    await page.getByLabel('Nombre o código escolar', { exact: true }).fill('FIC-001')
    let start = performance.now()
    await page.getByRole('button', { name: 'Buscar estudiantes', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Añadir FIC-001', exact: true })).toBeVisible()
    searches.push(performance.now() - start)
    await page.getByRole('button', { name: 'Añadir FIC-001', exact: true }).click()
    await page.getByLabel('Descripción objetiva').fill(`Medición ficticia ${i}`)
    start = performance.now()
    await page.getByRole('button', { name: 'Guardar registro', exact: true }).click()
    await expect(page.getByText('Registro guardado y confirmado.', { exact: true })).toBeVisible()
    saves.push(performance.now() - start)
  }
  const p95 = (xs: number[]) => [...xs].sort((a, b) => a - b)[Math.ceil(xs.length * .95) - 1]
  const result = { browser: browser.version(), timezone: 'America/Lima', transport: 'HTTPS loopback', students: studentCount, users: 1, sampleOnly: true, searches, saves, searchP95ms: p95(searches), saveP95ms: p95(saves) }
  await testInfo.attach('rendimiento-inicial.json', { body: JSON.stringify(result, null, 2), contentType: 'application/json' })
  expect(result.searchP95ms).toBeLessThanOrEqual(2000)
  expect(result.saveP95ms).toBeLessThanOrEqual(3000)
})
async function cambiarCuentaEnOtraPestana(page: Page, user: string) {
  await page.goto('/')
  await expect(page.getByRole('button', { name: 'Cerrar sesión', exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Acceso del personal' })).toBeVisible()
  await login(page, user)
}
test('dos pestañas: cambio de cuenta descarta detalle privado que llega tarde', async ({ page, context }) => {
  await login(page, 'docente')
  const text = `Privado de docente ${Date.now()}`
  await iniciar(page, text)
  await page.getByRole('button', { name: 'Guardar registro', exact: true }).click()
  await expect(page.getByText('Registro guardado y confirmado.', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Volver a mis registros', exact: true }).click()
  let release!: () => void, captured!: () => void
  const gate = new Promise<void>(r => { release = r }), ready = new Promise<void>(r => { captured = r })
  await page.route(/\/api\/notas\/[0-9a-f-]+$/, async route => { const response = await route.fetch(); captured(); await gate; await route.fulfill({ response }) }, { times: 1 })
  const finished = page.waitForResponse(/\/api\/notas\/[0-9a-f-]+$/)
  await page.getByRole('listitem').filter({ hasText: text }).getByRole('button', { name: 'Ver aporte', exact: true }).click()
  await ready
  const other = await context.newPage()
  try {
    await cambiarCuentaEnOtraPestana(other, 'ajeno')
    await expect(page.locator('header')).toContainText('Operador ficticio ajeno')
    release(); await finished
    await page.evaluate(() => new Promise<void>(r => requestAnimationFrame(() => requestAnimationFrame(() => r()))))
    await expect(page.locator('main')).not.toContainText(text)
    await expect(page.getByRole('heading', { name: 'Mi aporte', exact: true })).toHaveCount(0)
    await page.getByRole('button', { name: 'Nuevo registro', exact: true }).click()
    await expect(page.getByLabel('Descripción objetiva', { exact: true })).toHaveValue('')
  } finally { release(); await other.close() }
})
test('dos pestañas: error tardío de sesión anterior no cierra ni altera la nueva', async ({ page, context }) => {
  await login(page, 'auxiliar')
  let release!: () => void, captured!: () => void
  const gate = new Promise<void>(r => { release = r }), ready = new Promise<void>(r => { captured = r })
  await page.route('**/api/notas/consultar', async route => { captured(); await gate; await route.fulfill({ status: 401, contentType: 'application/json', body: '{"mensaje":"Sesión anterior terminada"}' }) }, { times: 1 })
  const finished = page.waitForResponse('**/api/notas/consultar')
  await page.getByRole('button', { name: 'Aplicar filtros', exact: true }).click(); await ready
  const other = await context.newPage()
  try {
    await cambiarCuentaEnOtraPestana(other, 'ajeno')
    await expect(page.locator('header')).toContainText('Operador ficticio ajeno')
    await page.getByRole('button', { name: 'Nuevo registro', exact: true }).click()
    await page.getByLabel('Descripción objetiva', { exact: true }).fill('Borrador exclusivo de la nueva cuenta')
    release(); await finished
    await page.evaluate(() => new Promise<void>(r => requestAnimationFrame(() => requestAnimationFrame(() => r()))))
    await expect(page.getByLabel('Descripción objetiva', { exact: true })).toHaveValue('Borrador exclusivo de la nueva cuenta')
    await expect(page.locator('header')).toContainText('Operador ficticio ajeno')
    await expect(page.getByRole('heading', { name: 'Acceso del personal' })).toHaveCount(0)
    page.on('dialog', d => d.accept())
  } finally { release(); await other.close() }
})
test('revocación técnica retira vistas de dos sesiones independientes', async ({ page, browser }) => {
  test.setTimeout(60000)
  const url = process.env.I01_TECH_DB_URL
  test.skip(!url || !process.env.I01_TECH_USER || !process.env.I01_TECH_PASSWORD, 'Requiere credenciales de la base ficticia en el proceso de pruebas, nunca en el navegador.')
  expect(url).toMatch(/\/i01_[a-zA-Z0-9_]+_(test|e2e)$/)
  await login(page, 'directivo')
  const second = await browser.newContext({ ignoreHTTPSErrors: true }), other = await second.newPage()
  try {
    await login(other, 'directivo')
    await promisify(execFile)('java', ['-jar', resolve('../backend/target/SistemaIncidenciasEscolaresBackend-0.1.0-SNAPSHOT.jar'), '--spring.main.web-application-type=none', '--i01.command=revoke', '--i01.login=directivo', '--i01.authorization=ENSAYO-E2E-REVOCACION'], { env: { ...process.env, DB_URL: url, DB_USER: process.env.I01_TECH_USER, DB_PASSWORD: process.env.I01_TECH_PASSWORD }, timeout: 30000 })
    for (const tab of [page, other]) {
      await tab.evaluate(() => window.dispatchEvent(new Event('focus')))
      await expect(tab.getByRole('heading', { name: 'Acceso del personal' })).toBeVisible()
      await expect(tab.getByRole('button', { name: 'Ver aporte', exact: true })).toHaveCount(0)
    }
  } finally { await second.close() }
})
test('foco verifica ocultando datos, conserva foco de la misma cuenta y falla cerrado', async ({ page }) => {
  await login(page, 'auxiliar')
  await iniciar(page, 'Borrador ficticio privado al verificar identidad')
  const description = page.getByLabel('Descripción objetiva', { exact: true })
  await description.focus()
  let release!: () => void, captured!: () => void
  let gate = new Promise<void>(r => { release = r }), ready = new Promise<void>(r => { captured = r })
  await page.route('**/api/acceso/sesion', async route => { const response = await route.fetch(); captured(); await gate; await route.fulfill({ response }) }, { times: 1 })
  await page.evaluate(() => window.dispatchEvent(new Event('focus'))); await ready
  await expect(description).not.toBeVisible(); release()
  await expect(description).toBeVisible(); await expect(description).toBeFocused()
  gate = new Promise<void>(r => { release = r }); ready = new Promise<void>(r => { captured = r })
  await page.route('**/api/acceso/sesion', async route => { captured(); await gate; await route.fulfill({ status: 503, contentType: 'application/json', body: '{"mensaje":"Fallo ficticio de verificación"}' }) }, { times: 1 })
  await page.evaluate(() => window.dispatchEvent(new Event('focus'))); await ready
  await expect(description).not.toBeVisible(); release()
  await expect(page.getByRole('heading', { name: 'Acceso del personal' })).toBeVisible()
  await page.getByLabel('Usuario', { exact: true }).fill('auxiliar')
  await page.getByLabel('Contraseña', { exact: true }).fill(password!)
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
  await expect(description).toHaveValue('Borrador ficticio privado al verificar identidad')
  page.on('dialog', d => d.accept())
})
