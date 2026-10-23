-- La localización y reserva alcanzan también a personas retiradas por una corrección ordinaria.
CREATE TABLE note_subject (
 note_id uuid NOT NULL REFERENCES note(id) ON DELETE CASCADE,
 student_id uuid NOT NULL REFERENCES student(id), PRIMARY KEY(note_id,student_id)
);
INSERT INTO note_subject SELECT note_id,student_id FROM note_participant;
CREATE INDEX subject_student ON note_subject(student_id,note_id);
