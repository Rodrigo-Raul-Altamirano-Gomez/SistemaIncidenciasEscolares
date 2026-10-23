import { test, expect } from '@playwright/test'
import type { Page } from '@playwright/test'
const password = process.env.I01_PASSWORD
if (!password) throw new Error('Define I01_PASSWORD con la contraseña ficticia de precarga.')
async function login(page: Page, user: string) {
  await page.goto('/')
  await page.getByLabel('Usuario', { exact: true }).fill(user)
  await page.getByLabel('Contraseña', { exact: true }).fill(password!)
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
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
  const result = { browser: browser.version(), timezone: 'America/Lima', transport: 'HTTPS loopback', students: 4, users: 1, sampleOnly: true, searches, saves, searchP95ms: p95(searches), saveP95ms: p95(saves) }
  await testInfo.attach('rendimiento-inicial.json', { body: JSON.stringify(result, null, 2), contentType: 'application/json' })
  expect(result.searchP95ms).toBeLessThanOrEqual(2000)
  expect(result.saveP95ms).toBeLessThanOrEqual(3000)
})
