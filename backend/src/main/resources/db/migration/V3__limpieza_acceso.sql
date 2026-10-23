-- La limpieza de sesiones/contadores vencidos no recorre todo el historial de accesos.
CREATE INDEX session_expiry ON auth_session(last_activity);
CREATE INDEX throttle_expiry ON login_throttle(started_at);
