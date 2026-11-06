ALTER TABLE employee ADD COLUMN public_id CHAR(36);

UPDATE employee SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE employee MODIFY COLUMN public_id CHAR(36) NOT NULL;
ALTER TABLE employee ADD CONSTRAINT uq_employee_public_id UNIQUE (public_id);
