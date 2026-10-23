import { useCallback, useEffect, useEffectEvent, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { api, ApiError, fecha, grupo, hoyPeru, papeles } from './api'
import type { Categoria, Entrada, Estudiante, Guardado, Registro, Sesion } from './api'
import './App.css'

const empty = (): Entrada => ({ anio: Number(hoyPeru().slice(0, 4)), fechaHecho: hoyPeru(), aproximada: false, hora: null, categoriaId: 'COTIDIANA', descripcion: '', lugar: null, comunicante: null, canal: null, participantes: [], accion: null, compromiso: null })
const campos: Record<string, string> = { anio: 'Año escolar', fechaHecho: 'Fecha del hecho', aproximada: 'Fecha aproximada', hora: 'Hora', categoriaId: 'Categoría', descripcion: 'Descripción', lugar: 'Lugar', comunicante: 'Comunicante', canal: 'Canal', participantes: 'Participaciones', accion: 'Acción realizada', compromiso: 'Compromiso' }
export default function App() {
  const [sesion, setSesion] = useState<Sesion | null>(null), [inicial, setInicial] = useState(true)
  const [usuario, setUsuario] = useState(''), [clave, setClave] = useState(''), [mensaje, setMensaje] = useState(''), [ocupado, setOcupado] = useState(false)
  const [pagina, setPagina] = useState<'registro' | 'propios' | 'detalle' | 'ayuda'>('propios')
  const [opciones, setOpciones] = useState<{ categorias: Categoria[]; anios: number[]; reiteracion: string } | null>(null)
  const [entrada, setEntrada] = useState<Entrada>(empty), [seleccionados, setSeleccionados] = useState<Estudiante[]>([])
  const [editando, setEditando] = useState<Registro | null>(null), [detalle, setDetalle] = useState<Registro | null>(null)
  const [cambios, setCambios] = useState(false), [pendiente, setPendiente] = useState<Guardado | null>(null), [incierto, setIncierto] = useState(false)
  const [lista, setLista] = useState<Registro[]>([]), [hayMas, setHayMas] = useState(false)
  const [filtros, setFiltros] = useState({ estudianteId: '', desde: '', hasta: '', categoriaId: '', anio: '', pagina: 0 })
  const [texto, setTexto] = useState(''), [buscaAnio, setBuscaAnio] = useState(''), [buscaGrado, setBuscaGrado] = useState(''), [buscaSeccion, setBuscaSeccion] = useState('')
  const [resultados, setResultados] = useState<Estudiante[]>([]), [reloj, setReloj] = useState(() => Date.now())
  const [filtroTexto, setFiltroTexto] = useState(''), [candidatos, setCandidatos] = useState<Estudiante[]>([])
  const draftOwner = useRef<string | null>(null), title = useRef<HTMLHeadingElement>(null)
  const actualizarExpira = useCallback((expira: string) => setSesion(s => s ? { ...s, expira } : s), [])
  const expire = useCallback(() => {
    setSesion(null); setLista([]); setDetalle(null); setResultados([]); setOpciones(null)
    setMensaje('La sesión terminó. Vuelve a identificarte. Tu registro sin guardar permanece oculto en esta pestaña para tu misma cuenta.')
  }, [])
  function error(e: unknown) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 403)) expire()
    else setMensaje(e instanceof Error ? e.message : 'No se pudo completar la operación.')
  }
  function limpiar() { setEntrada(empty()); setSeleccionados([]); setEditando(null); setCambios(false); setPendiente(null); setIncierto(false) }
  function limpiarBusqueda() { setTexto(''); setBuscaAnio(''); setBuscaGrado(''); setBuscaSeccion(''); setFiltroTexto(''); setCandidatos([]); setFiltros({ estudianteId: '', desde: '', hasta: '', categoriaId: '', anio: '', pagina: 0 }) }
  function cambiar(next: Entrada) { setEntrada(next); setCambios(true); setPendiente(null) }
  function navegar(next: typeof pagina) {
    if (pagina === 'registro' && cambios && !window.confirm('Hay cambios sin guardar. ¿Deseas descartarlos?')) return
    if (pagina === 'registro' && cambios) limpiar()
    setPagina(next); setMensaje('')
  }
  useEffect(() => { api<Sesion>('/acceso/sesion').then(s => { setSesion(s); draftOwner.current = s.usuario.id }).catch(() => {}).finally(() => setInicial(false)) }, [])
  const verificarSesion = useEffectEvent(() => {
    api<Sesion>('/acceso/sesion').then(s => { setSesion(s); if (!s.usuario.roles.some(r => ['DOCENTE', 'AUXILIAR', 'DIRECTIVO', 'TOE', 'COMITE', 'DIRECTOR'].includes(r))) expire() }).catch(e => { if (e instanceof ApiError && e.status === 401) expire() })
  })
  const cargarOpciones = useEffectEvent(() => { if (sesion) api<typeof opciones>('/opciones', 'GET', undefined, sesion.csrf, actualizarExpira).then(setOpciones).catch(error) })
  const comprobarTiempo = useEffectEvent(() => { if (sesion && reloj >= Date.parse(sesion.expira)) expire() })
  const sesionId = sesion?.usuario.id
  useEffect(() => {
    const prevent = (e: BeforeUnloadEvent) => { if (cambios) { e.preventDefault(); e.returnValue = '' } }
    window.addEventListener('beforeunload', prevent); return () => window.removeEventListener('beforeunload', prevent)
  }, [cambios])
  useEffect(() => { title.current?.focus() }, [pagina, sesion?.usuario.id])
  useEffect(() => {
    if (!sesionId) return
    const timer = window.setInterval(() => setReloj(Date.now()), 1000)
    const check = window.setInterval(() => {
      verificarSesion()
    }, 30000)
    return () => { clearInterval(timer); clearInterval(check) }
  }, [sesionId])
  useEffect(() => { queueMicrotask(comprobarTiempo) }, [sesion?.expira, reloj])
  useEffect(() => { queueMicrotask(cargarOpciones) }, [sesionId])
  async function entrar(e: FormEvent) {
    e.preventDefault(); setOcupado(true); setMensaje('')
    try {
      const s = await api<Sesion>('/acceso/login', 'POST', { usuario, clave })
      if (draftOwner.current && draftOwner.current !== s.usuario.id) { limpiar(); limpiarBusqueda(); setPagina('propios') }
      draftOwner.current = s.usuario.id; setReloj(Date.now()); setSesion(s); setClave('')
    } catch (e) { error(e) } finally { setOcupado(false) }
  }
  async function salir() {
    if (cambios && !window.confirm('Cerrar sesión descartará el texto sin guardar. ¿Deseas continuar?')) return
    setOcupado(true)
    try { await api('/acceso/logout', 'POST', {}, sesion?.csrf); limpiar(); limpiarBusqueda(); setSesion(null); setLista([]); setDetalle(null); setResultados([]); setOpciones(null); setUsuario(''); draftOwner.current = null; setMensaje('Sesión cerrada.'); setPagina('propios') }
    catch (e) { error(e) } finally { setOcupado(false) }
  }
  async function buscar(e: FormEvent) {
    e.preventDefault(); setOcupado(true); setMensaje(''); setResultados([])
    try { setResultados(await api('/estudiantes/buscar', 'POST', { texto, anio: buscaAnio ? Number(buscaAnio) : null, grado: buscaGrado || null, seccion: buscaSeccion || null }, sesion?.csrf, actualizarExpira)) }
    catch (e) { error(e) } finally { setOcupado(false) }
  }
  function agregar(e: Estudiante) {
    if (seleccionados.some(s => s.id === e.id)) return
    const cat = opciones?.categorias.find(c => c.id === entrada.categoriaId)
    const papel = seleccionados.length === 0 && cat?.naturaleza === 'DISCIPLINARIA' ? 'ATRIBUIDA' : 'DESCONOCIDO'
    setSeleccionados([...seleccionados, e]); cambiar({ ...entrada, participantes: [...entrada.participantes, { estudianteId: e.id, papel }] })
  }
  async function guardar(e: FormEvent) {
    e.preventDefault(); setOcupado(true); setMensaje('')
    const payload = pendiente ?? { solicitudId: crypto.randomUUID(), version: editando?.version ?? null, entrada }
    setPendiente(payload)
    try {
      const saved = await api<Registro>(editando ? `/notas/${editando.id}` : '/notas', editando ? 'PUT' : 'POST', payload, sesion?.csrf, actualizarExpira)
      limpiar(); setDetalle(saved); setPagina('detalle'); setMensaje('Registro guardado y confirmado.')
    } catch (e) { setIncierto(!(e instanceof ApiError) || e.status === 0 || e.status >= 500); error(e) } finally { setOcupado(false) }
  }
  async function consultar(page = 0) {
    setOcupado(true); setMensaje('')
    try {
      const result = await api<{ registros: Registro[]; hayMas: boolean }>('/notas/consultar', 'POST', { ...filtros, estudianteId: filtros.estudianteId || null, desde: filtros.desde || null, hasta: filtros.hasta || null, categoriaId: filtros.categoriaId || null, anio: filtros.anio ? Number(filtros.anio) : null, pagina: page }, sesion?.csrf, actualizarExpira)
      setLista(result.registros); setHayMas(result.hayMas); setFiltros(f => ({ ...f, pagina: page }))
    } catch (e) { setLista([]); error(e) } finally { setOcupado(false) }
  }
  const consultarAlEntrar = useEffectEvent(() => { if (sesion && pagina === 'propios' && opciones) void consultar(filtros.pagina) })
  useEffect(() => { queueMicrotask(consultarAlEntrar) }, [pagina, opciones])
  async function buscarFiltro() {
    setOcupado(true)
    try { setCandidatos(await api('/estudiantes/buscar', 'POST', { texto: filtroTexto }, sesion?.csrf, actualizarExpira)) } catch(e) { error(e) } finally { setOcupado(false) }
  }
  async function abrir(id: string) {
    setOcupado(true); setMensaje('')
    try { const result = await api<Registro>(`/notas/${id}`, 'GET', undefined, sesion?.csrf, actualizarExpira); setDetalle(result); setPagina('detalle') }
    catch (e) { setDetalle(null); error(e) } finally { setOcupado(false) }
  }
  function editar(r: Registro) {
    setEntrada(structuredClone(r.cuerpo.entrada)); setSeleccionados(r.cuerpo.participantes.map(p => ({ id: p.estudianteId, nombre: p.nombre, codigo: p.codigo, anio: r.cuerpo.entrada.anio, grado: p.grado, seccion: p.seccion })))
    setEditando(r); setCambios(false); setPendiente(null); setIncierto(false); setPagina('registro'); setMensaje('')
  }
  if (inicial) return <main><p role="status">Comprobando sesión…</p></main>
  if (!sesion) return <main className="login"><p className="eyebrow">Incidencias escolares · ensayo ficticio I01</p><h1 ref={title} tabIndex={-1}>Acceso del personal</h1><p>Registra una incidencia cotidiana y recupera tus propios aportes.</p>
    {mensaje && <p className="message" role="alert">{mensaje}</p>}<form onSubmit={entrar}><label>Usuario<input autoComplete="username" value={usuario} onChange={e => setUsuario(e.target.value)} maxLength={80} required /></label><label>Contraseña<input type="password" autoComplete="current-password" value={clave} onChange={e => setClave(e.target.value)} maxLength={72} required /></label><button disabled={ocupado}>{ocupado ? 'Comprobando…' : 'Iniciar sesión'}</button></form>
    <p>Para recuperar acceso, contacta al responsable operativo. Comprobará tu identidad y retirará las sesiones anteriores por el procedimiento autorizado.</p><p>Los datos de esta versión son ficticios. Usa una cuenta individual.</p></main>
  const minutos = Math.max(0, Math.ceil((Date.parse(sesion.expira) - reloj) / 60000))
  return <><a className="skip" href="#contenido">Ir al contenido</a><header><div><strong>Incidencias escolares</strong><span>Registro cotidiano · I01 · datos ficticios</span></div><div><span>{sesion.usuario.nombre}</span><button onClick={salir} disabled={ocupado}>Cerrar sesión</button></div></header><nav aria-label="Principal"><button aria-current={pagina === 'propios' ? 'page' : undefined} onClick={() => navegar('propios')}>Mis registros</button><button aria-current={pagina === 'registro' ? 'page' : undefined} onClick={() => { if (pagina !== 'registro') { navegar('registro'); limpiar() } }}>Nuevo registro</button><button onClick={() => navegar('ayuda')}>Ayuda</button></nav>
    <main id="contenido"><h1 ref={title} tabIndex={-1}>{pagina === 'registro' ? editando ? 'Corregir mi registro' : 'Registrar una incidencia cotidiana' : pagina === 'propios' ? 'Mis registros cotidianos' : pagina === 'detalle' ? 'Mi aporte' : 'Ayuda de registro'}</h1>
      {minutos <= 2 && <p className="message" role="alert">Tu sesión terminará en {minutos} minuto(s) sin operaciones. El texto quedará oculto en esta pestaña. Vuelve a autenticarte con la misma cuenta para continuarlo.</p>}{mensaje && <p className="message" role="alert">{mensaje}</p>}
      {pagina === 'registro' && <><p>Un guardado conserva el hecho, sus participantes y la acción inmediata. No requiere aprobación de TOE.</p><section aria-labelledby="buscar-titulo"><h2 id="buscar-titulo">1. Localizar estudiantes</h2><form className="search" onSubmit={buscar}><label>Nombre o código escolar<input value={texto} onChange={e => setTexto(e.target.value)} maxLength={100} /></label><label>Año de matrícula<select value={buscaAnio} onChange={e => setBuscaAnio(e.target.value)}><option value="">Actuales y anteriores</option>{opciones?.anios.map(a => <option key={a}>{a}</option>)}</select></label><label>Grado (opcional)<input value={buscaGrado} onChange={e => setBuscaGrado(e.target.value)} maxLength={32} /></label><label>Sección (opcional)<input value={buscaSeccion} onChange={e => setBuscaSeccion(e.target.value)} maxLength={32} /></label><button disabled={ocupado || incierto}>Buscar estudiantes</button></form><ul className="results">{resultados.slice(0, 50).map((e, i) => <li key={`${e.id}-${e.anio}-${i}`}><span><strong>{e.nombre}</strong> · {e.codigo}<br />{e.anio ?? 'Sin matrícula'} · {grupo(e)}</span><button disabled={incierto || seleccionados.some(s => s.id === e.id)} onClick={() => agregar(e)}>Añadir {e.codigo}</button></li>)}</ul>{resultados.length > 50 && <p>Hay más coincidencias. Afina los criterios para localizar al estudiante.</p>}</section>
        <form onSubmit={guardar}><fieldset disabled={ocupado || incierto}><legend>2. Hecho y papeles individuales</legend>{seleccionados.length === 0 && <p>Busca y añade al menos un estudiante. No se exige documento de identidad.</p>}{seleccionados.map(e => <div className="participant" key={e.id}><div><strong>{e.nombre}</strong> · {e.codigo}<br /><small>Referencia de matrícula: {e.anio ?? 'no informada'} · {grupo(e)}. Al guardar se recuperará el grupo conocido a la fecha del hecho.</small></div><label>Papel de {e.codigo}<select value={entrada.participantes.find(p => p.estudianteId === e.id)?.papel} onChange={ev => cambiar({ ...entrada, participantes: entrada.participantes.map(p => p.estudianteId === e.id ? { ...p, papel: ev.target.value } : p) })}>{Object.entries(papeles).filter(([key]) => key !== 'ATRIBUIDA' || opciones?.categorias.find(c => c.id === entrada.categoriaId)?.naturaleza === 'DISCIPLINARIA').map(([key, value]) => <option key={key} value={key}>{value}</option>)}</select></label><p className="hint">Papel seleccionado: {papeles[entrada.participantes.find(p => p.estudianteId === e.id)?.papel ?? "DESCONOCIDO"]}</p><button type="button" onClick={() => { setSeleccionados(seleccionados.filter(s => s.id !== e.id)); cambiar({ ...entrada, participantes: entrada.participantes.filter(p => p.estudianteId !== e.id) }) }}>Quitar {e.codigo}</button></div>)}<p className="hint">La atribución es la constancia de su autor; no es corroboración de TOE ni sanción. Añadir otro estudiante propone papel desconocido.</p>
          <div className="grid"><label>Año escolar<select required value={entrada.anio} onChange={e => cambiar({ ...entrada, anio: Number(e.target.value) })}>{opciones?.anios.map(a => <option key={a}>{a}</option>)}</select></label><label>Fecha del hecho<input type="date" required max={hoyPeru()} value={entrada.fechaHecho} onChange={e => cambiar({ ...entrada, fechaHecho: e.target.value })} /></label><label className="check"><input type="checkbox" checked={entrada.aproximada} onChange={e => cambiar({ ...entrada, aproximada: e.target.checked, hora: e.target.checked ? null : entrada.hora })} /> Fecha aproximada</label><label>Hora conocida (opcional)<input type="time" disabled={entrada.aproximada} value={entrada.hora ?? ''} onChange={e => cambiar({ ...entrada, hora: e.target.value || null })} /></label></div>
          <label>Categoría<select value={entrada.categoriaId} onChange={e => cambiar({ ...entrada, categoriaId: e.target.value, participantes: entrada.participantes.map(p => p.papel === 'ATRIBUIDA' && e.target.value !== 'COTIDIANA' ? { ...p, papel: 'DESCONOCIDO' } : p) })}>{opciones?.categorias.map(c => <option key={c.id} value={c.id}>{c.nombre} · {c.gravedad === 'LEVE' ? 'leve' : 'por clasificar'}</option>)}</select></label><p className="hint">Categoría seleccionada: {opciones?.categorias.find(c => c.id === entrada.categoriaId)?.nombre}</p><label htmlFor="descripcion">Descripción objetiva</label><textarea id="descripcion" required maxLength={4000} rows={5} value={entrada.descripcion} onChange={e => cambiar({ ...entrada, descripcion: e.target.value })} /><details><summary>Datos adicionales conocidos (opcionales)</summary><div className="grid">{(['lugar', 'comunicante', 'canal'] as const).map(k => <label key={k}>{campos[k]}<input maxLength={k === 'canal' ? 80 : 160} value={entrada[k] ?? ''} onChange={e => cambiar({ ...entrada, [k]: e.target.value || null })} /></label>)}</div></details>
          <h2>3. Acción inmediata (opcional)</h2><label className="check"><input type="checkbox" checked={!!entrada.accion} onChange={e => cambiar({ ...entrada, accion: e.target.checked ? { contenido: '', fechaRealizada: entrada.fechaHecho, resultado: null } : null, compromiso: null })} /> Hubo una acción realizada</label>{entrada.accion && <><label htmlFor="accion">Acción realizada</label><textarea id="accion" required maxLength={2000} value={entrada.accion.contenido} onChange={e => cambiar({ ...entrada, accion: { ...entrada.accion!, contenido: e.target.value } })} /><div className="grid"><label>Fecha de realización<input type="date" required max={hoyPeru()} min={entrada.fechaHecho} value={entrada.accion.fechaRealizada} onChange={e => cambiar({ ...entrada, accion: { ...entrada.accion!, fechaRealizada: e.target.value } })} /></label><label>Resultado (opcional)<input maxLength={1000} value={entrada.accion.resultado ?? ''} onChange={e => cambiar({ ...entrada, accion: { ...entrada.accion!, resultado: e.target.value || null } })} /></label></div><label className="check"><input type="checkbox" checked={!!entrada.compromiso} onChange={e => cambiar({ ...entrada, compromiso: e.target.checked ? { contenido: '', asumidoPor: '', fechaObjetivo: null } : null })} /> Hubo un compromiso acordado</label>{entrada.compromiso && <><label htmlFor="compromiso">Compromiso</label><textarea id="compromiso" required maxLength={2000} value={entrada.compromiso.contenido} onChange={e => cambiar({ ...entrada, compromiso: { ...entrada.compromiso!, contenido: e.target.value } })} /><div className="grid"><label>Persona que lo asume<input required maxLength={160} value={entrada.compromiso.asumidoPor} onChange={e => cambiar({ ...entrada, compromiso: { ...entrada.compromiso!, asumidoPor: e.target.value } })} /></label><label>Fecha objetivo (solo si se acordó)<input type="date" value={entrada.compromiso.fechaObjetivo ?? ''} onChange={e => cambiar({ ...entrada, compromiso: { ...entrada.compromiso!, fechaObjetivo: e.target.value || null } })} /></label></div></>}</>}
        </fieldset><p className="hint">{opciones?.reiteracion}</p>{incierto && <p role="status">El resultado aún no está confirmado. Reintenta con los mismos datos para comprobarlo sin duplicar el registro.</p>}<div className="actions"><button disabled={ocupado || seleccionados.length === 0}>{ocupado ? 'Guardando…' : incierto ? 'Comprobar guardado' : editando ? 'Guardar corrección' : 'Guardar registro'}</button>{editando && <button type="button" disabled={ocupado} onClick={() => { if (!cambios || window.confirm('Abrir la versión actual descartará esta edición. ¿Continuar?')) { const id = editando.id; limpiar(); void abrir(id) } }}>Abrir versión actual</button>}</div></form></>}
      {pagina === 'propios' && <><p>Solo aparecen tus aportes. Los filtros usan la fecha del hecho y el año escolar explícito.</p><form className="filters" onSubmit={e => { e.preventDefault(); void consultar() }}><label>Buscar estudiante para filtrar<input value={filtroTexto} maxLength={100} onChange={e => setFiltroTexto(e.target.value)} /></label><button type="button" disabled={ocupado} onClick={buscarFiltro}>Localizar para filtro</button><label>Estudiante (opcional)<select value={filtros.estudianteId} onChange={e => setFiltros({ ...filtros, estudianteId: e.target.value })}><option value="">Todos</option>{candidatos.slice(0,50).map((p,i) => <option key={`${p.id}-${i}`} value={p.id}>{p.nombre} · {p.codigo} · {p.anio ?? "Sin matrícula"} · {grupo(p)}</option>)}{filtros.estudianteId && !candidatos.some(p => p.id === filtros.estudianteId) && <option value={filtros.estudianteId}>Estudiante seleccionado desde el registro</option>}</select></label><label>Desde<input type="date" value={filtros.desde} onChange={e => setFiltros({ ...filtros, desde: e.target.value })} /></label><label>Hasta<input type="date" value={filtros.hasta} onChange={e => setFiltros({ ...filtros, hasta: e.target.value })} /></label><label>Categoría<select value={filtros.categoriaId} onChange={e => setFiltros({ ...filtros, categoriaId: e.target.value })}><option value="">Todas</option>{opciones?.categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}</select></label><label>Año escolar<select value={filtros.anio} onChange={e => setFiltros({ ...filtros, anio: e.target.value })}><option value="">Todos</option>{opciones?.anios.map(a => <option key={a}>{a}</option>)}</select></label><button disabled={ocupado}>{ocupado ? 'Consultando…' : 'Aplicar filtros'}</button></form><p role="status">{lista.length === 0 ? 'Sin registros para estos criterios.' : `${lista.length} registros en esta página.`}</p><ul className="records">{lista.map(r => <li key={r.id}><div><strong>{fecha(r.cuerpo.entrada.fechaHecho)}{r.cuerpo.entrada.aproximada ? ' · aproximada' : ''}</strong><span>{r.cuerpo.categoriaNombre} · {r.cuerpo.entrada.anio}</span><p>{r.cuerpo.entrada.descripcion}</p><span>{r.cuerpo.participantes.map(p => `${p.nombre} (${p.codigo})`).join(' · ')}</span>{r.cuerpo.participantes.map(p => <button key={p.estudianteId} onClick={() => setFiltros({ ...filtros, estudianteId: p.estudianteId })}>Filtrar por {p.codigo}</button>)}</div><button onClick={() => abrir(r.id)}>Ver aporte</button></li>)}</ul><div className="actions"><button disabled={ocupado || filtros.pagina === 0} onClick={() => consultar(filtros.pagina - 1)}>Anterior</button><span>Página {filtros.pagina + 1}</span><button disabled={ocupado || !hayMas} onClick={() => consultar(filtros.pagina + 1)}>Siguiente</button></div></>}
      {pagina === 'detalle' && detalle && <><p className="hint">Reservado · aporte propio · versión {detalle.version}. {opciones?.reiteracion}</p><dl><dt>Fecha del hecho</dt><dd>{fecha(detalle.cuerpo.entrada.fechaHecho)}{detalle.cuerpo.entrada.aproximada ? ' (aproximada)' : ''} · {detalle.cuerpo.entrada.hora ?? 'Hora no informada'} · año {detalle.cuerpo.entrada.anio}</dd><dt>Autor y fecha de registro</dt><dd>{detalle.autor} · {fecha(detalle.registrada)}</dd><dt>Categoría guardada</dt><dd>{detalle.cuerpo.categoriaNombre} · {detalle.cuerpo.gravedad} · {detalle.cuerpo.naturaleza}</dd><dt>Descripción</dt><dd className="prose">{detalle.cuerpo.entrada.descripcion}</dd>{(['lugar', 'comunicante', 'canal'] as const).map(k => <div key={k}><dt>{campos[k]}</dt><dd>{detalle.cuerpo.entrada[k] ?? 'No informado'}</dd></div>)}</dl><h2>Participantes del hecho</h2><ul>{detalle.cuerpo.participantes.map(p => <li key={p.estudianteId}>{p.nombre} · {p.codigo} · grupo histórico: {grupo(p)} · {papeles[p.papel]}</li>)}</ul><h2>Acción y compromiso inmediato</h2>{detalle.cuerpo.entrada.accion ? <><p className="prose">{detalle.cuerpo.entrada.accion.contenido}</p><p>Realizada el {fecha(detalle.cuerpo.entrada.accion.fechaRealizada)} · resultado: {detalle.cuerpo.entrada.accion.resultado ?? 'no informado'}</p>{detalle.cuerpo.entrada.compromiso && <p className="prose">{detalle.cuerpo.entrada.compromiso.contenido} · asumido por {detalle.cuerpo.entrada.compromiso.asumidoPor} · fecha objetivo: {fecha(detalle.cuerpo.entrada.compromiso.fechaObjetivo ?? '')}</p>}</> : <p>No se registró acción inmediata.</p>}<button onClick={() => editar(detalle)}>Corregir mi aporte</button><h2>Rectificaciones automáticas</h2>{detalle.rectificaciones.length === 0 && <p>No hay rectificaciones.</p>}{detalle.rectificaciones.map(r => <section key={r.version}><h3>Versión {r.version} · {r.autor} · {fecha(r.fecha)}</h3>{r.cambios.map(c => <div key={c.campo}><strong>{campos[c.campo] ?? c.campo}</strong><p className="prose">Antes: {mostrar(c.antes)}</p><p className="prose">Después: {mostrar(c.despues)}</p></div>)}</section>)}<button onClick={() => navegar('propios')}>Volver a mis registros</button></>}
      {pagina === 'ayuda' && <><p>Busca por nombre o código escolar, también en años anteriores. El nombre se conserva con sus tildes; los homónimos se distinguen por código, año y grupo. Documento y contactos están reservados.</p><p>El primer estudiante de una categoría disciplinaria leve propone una conducta atribuida por ti: revisa su papel. Los demás se añaden con papel desconocido; afectados y testigos no heredan atribución.</p><p>Fecha del hecho y fecha de registro son distintas. Puedes registrar hechos pasados y fechas aproximadas sin inventar hora o grupo. Una acción realizada y su compromiso son opcionales y se guardan con el hecho.</p><p>Consulta y corrige tus aportes. La corrección conserva automáticamente autor, fecha y diferencias; no pide una justificación rutinaria.</p><p>Si se pierde la conexión, conserva la pestaña y usa «Comprobar guardado». Si otra sesión corrigió el aporte, conserva tu texto y revisa la versión actual. No se promete trabajo sin conexión ni conservación al cerrar la pestaña.</p><p>La sesión vence tras veinte minutos sin operaciones con datos. Recibirás un aviso antes; el texto sin guardar se oculta hasta que vuelvas a entrar con la misma cuenta. Cierra sesión al dejar el puesto.</p><p>Durante una caída, continúa las acciones urgentes por los canales institucionales y regístralas después con sus fechas reales. Esta versión solo ofrece el registro cotidiano no grave: no reemplaza Libro de Incidencias ni SíseVe.</p><p>El catálogo y los datos son ficticios; no representan políticas escolares aprobadas. No hay parámetros de reiteración aprobados ni cálculo automático activo.</p></>}
    </main><footer>Información reservada · Fechas en Perú (America/Lima) · Usa solo datos ficticios</footer></>
}
function mostrar(value: unknown): string {
  if (value === null || value === '') return 'No informado'
  if (typeof value === 'boolean') return value ? 'Sí' : 'No'
  if (Array.isArray(value)) return value.map(mostrar).join('\n')
  if (typeof value === 'object') {
    const o = value as Record<string, unknown>
    if (typeof o.nombre === 'string' && typeof o.papel === 'string') return `${o.nombre} (${o.codigo}) · ${papeles[o.papel] ?? o.papel}`
    const etiquetas: Record<string, string> = { ...campos, contenido: 'Contenido', fechaRealizada: 'Fecha de realización', resultado: 'Resultado', asumidoPor: 'Persona que lo asume', fechaObjetivo: 'Fecha objetivo' }
    return Object.entries(o).map(([key, val]) => `${etiquetas[key] ?? key}: ${mostrar(val)}`).join('\n')
  }
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) return fecha(value)
  return String(value)
}
