CREATE TABLE app_user (
 id uuid PRIMARY KEY, login varchar(80) NOT NULL UNIQUE, name varchar(120) NOT NULL,
 password_hash text NOT NULL, active boolean NOT NULL DEFAULT true, access_version integer NOT NULL DEFAULT 1
);
CREATE TABLE user_role (
 user_id uuid REFERENCES app_user(id), role varchar(24) NOT NULL,
 valid_from date NOT NULL, valid_to date, PRIMARY KEY(user_id, role, valid_from),
 CHECK(valid_to IS NULL OR valid_to >= valid_from)
);
CREATE TABLE auth_session (
 token_hash char(64) PRIMARY KEY, user_id uuid NOT NULL REFERENCES app_user(id),
 csrf_token varchar(64) NOT NULL, access_version integer NOT NULL,
 created_at timestamptz NOT NULL, last_activity timestamptz NOT NULL
);
CREATE TABLE login_throttle (key char(64) PRIMARY KEY, attempts integer NOT NULL, started_at timestamptz NOT NULL);
CREATE TABLE school_year (year integer PRIMARY KEY CHECK(year BETWEEN 2000 AND 2100));
CREATE TABLE student (
 id uuid PRIMARY KEY, school_code varchar(32) NOT NULL UNIQUE,
 identity_enc text NOT NULL, blocked boolean NOT NULL DEFAULT false
);
CREATE TABLE enrollment (
 student_id uuid NOT NULL REFERENCES student(id), year integer NOT NULL REFERENCES school_year(year),
 grade varchar(32), section varchar(32), effective_from date NOT NULL, effective_to date,
 PRIMARY KEY(student_id,year,effective_from), CHECK(effective_to IS NULL OR effective_to >= effective_from)
);
CREATE TABLE category (
 id varchar(32) PRIMARY KEY, name varchar(100) NOT NULL,
 nature varchar(24) NOT NULL CHECK(nature IN ('DISCIPLINARIA','OTRA')),
 severity varchar(24) NOT NULL CHECK(severity IN ('LEVE','POR_CLASIFICAR')), active boolean NOT NULL DEFAULT true
);
CREATE TABLE note (
 id uuid PRIMARY KEY, author_id uuid NOT NULL REFERENCES app_user(id),
 school_year integer NOT NULL REFERENCES school_year(year), category_id varchar(32) NOT NULL REFERENCES category(id),
 event_date date NOT NULL, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 version integer NOT NULL CHECK(version > 0), body_enc text NOT NULL, blocked boolean NOT NULL DEFAULT false
);
CREATE INDEX note_author_date ON note(author_id,event_date DESC,id);
CREATE TABLE note_participant (
 note_id uuid NOT NULL REFERENCES note(id) ON DELETE CASCADE, student_id uuid NOT NULL REFERENCES student(id),
 role varchar(24) NOT NULL CHECK(role IN ('ATRIBUIDA','AFECTADO','TESTIGO','OTRO','DESCONOCIDO')),
 PRIMARY KEY(note_id, student_id)
);
CREATE INDEX participant_student ON note_participant(student_id,note_id);
CREATE TABLE note_revision (
 id bigserial PRIMARY KEY, note_id uuid NOT NULL REFERENCES note(id) ON DELETE CASCADE,
 version integer NOT NULL, actor_id uuid NOT NULL REFERENCES app_user(id), at timestamptz NOT NULL,
 changes_enc text NOT NULL, UNIQUE(note_id,version)
);
CREATE TABLE request_receipt (
 actor_id uuid NOT NULL REFERENCES app_user(id), request_id uuid NOT NULL,
 fingerprint char(64) NOT NULL, note_id uuid NOT NULL REFERENCES note(id) ON DELETE CASCADE,
 PRIMARY KEY(actor_id,request_id)
);
CREATE TABLE audit_event (
 id bigserial PRIMARY KEY, actor_id uuid REFERENCES app_user(id), at timestamptz NOT NULL DEFAULT now(),
 operation varchar(40) NOT NULL, result varchar(32) NOT NULL, resource uuid, metadata jsonb NOT NULL DEFAULT '{}'
);
CREATE INDEX audit_lookup ON audit_event(at,actor_id,operation,resource);
CREATE FUNCTION protect_audit() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.at > now() - interval '2 years' THEN RAISE EXCEPTION 'Protected evidence'; END IF;
 RETURN OLD;
END $$;
CREATE TRIGGER immutable_audit BEFORE UPDATE OR DELETE ON audit_event FOR EACH ROW EXECUTE FUNCTION protect_audit();
