export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) { super(message); this.status = status }
}
export type Usuario = { id: string; nombre: string; roles: string[] }
export type Sesion = { usuario: Usuario; csrf: string; expira: string }
export type Estudiante = { id: string; nombre: string; codigo: string; anio: number | null; grado: string | null; seccion: string | null }
export type Categoria = { id: string; nombre: string; naturaleza: string; gravedad: string }
export type Entrada = {
  anio: number; fechaHecho: string; aproximada: boolean; hora: string | null;
  categoriaId: string; descripcion: string; lugar: string | null; comunicante: string | null; canal: string | null;
  participantes: { estudianteId: string; papel: string }[];
  accion: { contenido: string; fechaRealizada: string; resultado: string | null } | null;
  compromiso: { contenido: string; asumidoPor: string; fechaObjetivo: string | null } | null;
}
export type Registro = {
  id: string; autorId: string; autor: string; version: number; registrada: string; actualizada: string;
  cuerpo: { entrada: Entrada; categoriaNombre: string; naturaleza: string; gravedad: string;
    participantes: { estudianteId: string; nombre: string; codigo: string; grado: string | null; seccion: string | null; papel: string }[] };
  rectificaciones: { version: number; autor: string; fecha: string; cambios: { campo: string; antes: unknown; despues: unknown }[] }[];
}
export type Guardado = { solicitudId: string; version: number | null; entrada: Entrada }
export const hoyPeru = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
export const fecha = (s: string) => s ? (s.length === 10 ? s.split('-').reverse().join('/') : new Intl.DateTimeFormat('es-PE', { timeZone: 'America/Lima', dateStyle: 'short', timeStyle: 'short' }).format(new Date(s))) : 'No informada'
export const papeles: Record<string, string> = { ATRIBUIDA: 'Conducta cotidiana atribuida por mí', AFECTADO: 'Afectado/a', TESTIGO: 'Testigo', OTRO: 'Otro interviniente', DESCONOCIDO: 'Papel desconocido' }
export const grupo = (e: { grado: string | null; seccion: string | null }) => e.grado ? `${e.grado}° ${e.seccion ?? 'sección no informada'}` : 'Grupo no informado'
export async function api<T>(path: string, method = 'GET', body?: unknown, csrf?: string, onExpiry?: (value: string) => void): Promise<T> {
  let response: Response
  try {
    response = await fetch(`/api${path}`, { method, credentials: 'same-origin', cache: 'no-store',
      headers: { 'Content-Type': 'application/json', 'X-I01-Client': 'true', ...(csrf ? { 'X-CSRF-Token': csrf } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body) })
  } catch { throw new ApiError(0, 'Se perdió la conexión. Conservamos tu texto en esta pestaña. Reintenta cuando vuelva el servicio.') }
  const serverTime = Date.parse(response.headers.get('Date') ?? '')
  const localDeadline = (value: string) => Number.isFinite(serverTime) ? new Date(Date.now() + Date.parse(value) - serverTime).toISOString() : value
  const expires = response.headers.get('X-Session-Expires')
  if (expires) onExpiry?.(localDeadline(expires))
  const data = await response.json().catch(() => ({}))
  if (data.expira) data.expira = localDeadline(data.expira)
  if (!response.ok) throw new ApiError(response.status, data.mensaje ?? 'No se pudo confirmar la operación. Conserva tu texto y reintenta.')
  return data as T
}
