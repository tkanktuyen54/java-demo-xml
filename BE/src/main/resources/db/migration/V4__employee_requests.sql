CREATE TABLE account_role (
  user_id BIGINT NOT NULL,
  role VARCHAR(30) NOT NULL,
  PRIMARY KEY (user_id, role),
  CONSTRAINT fk_account_role_user FOREIGN KEY (user_id) REFERENCES tbl_user(id),
  CONSTRAINT ck_account_role CHECK (role IN ('HR', 'HR_APPROVER', 'ADMIN'))
);

CREATE TABLE employee (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_code VARCHAR(40) UNIQUE,
  full_name VARCHAR(200) NOT NULL,
  department VARCHAR(200) NOT NULL,
  position VARCHAR(200) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'Active',
  profile_json MEDIUMTEXT NOT NULL,
  version BIGINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP(6) NOT NULL,
  last_modified_at TIMESTAMP(6),
  CONSTRAINT ck_employee_status CHECK (status IN ('Active', 'Suspended', 'Terminated'))
);

CREATE TABLE employee_contact (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  email VARCHAR(254) UNIQUE,
  phone_number VARCHAR(30),
  is_primary BOOLEAN NOT NULL,
  CONSTRAINT fk_employee_contact_employee FOREIGN KEY (employee_id) REFERENCES employee(id)
);

CREATE INDEX ix_employee_department ON employee(department);
CREATE INDEX ix_employee_contact_employee ON employee_contact(employee_id);

CREATE TABLE employee_request (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(30) NOT NULL,
  state VARCHAR(30) NOT NULL DEFAULT 'Draft',
  subject_id BIGINT,
  requester_id BIGINT NOT NULL,
  baseline_version BIGINT,
  baseline_json MEDIUMTEXT,
  target_json MEDIUMTEXT,
  target_name VARCHAR(200),
  version BIGINT NOT NULL DEFAULT 1,
  revision_number INT NOT NULL DEFAULT 0,
  latest_revision_id BIGINT,
  reason VARCHAR(2000),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP(6) NOT NULL,
  last_modified_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_employee_request_subject FOREIGN KEY (subject_id) REFERENCES employee(id),
  CONSTRAINT fk_employee_request_requester FOREIGN KEY (requester_id) REFERENCES tbl_user(id),
  CONSTRAINT ck_employee_request_type CHECK (type IN ('Creation', 'Update')),
  CONSTRAINT ck_employee_request_state CHECK (state IN (
    'Draft', 'PendingApproval', 'Rejected', 'Revoked', 'Approved', 'Cancelled'
  ))
);

CREATE TABLE employee_request_revision (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  revision_number INT NOT NULL,
  baseline_version BIGINT,
  baseline_json MEDIUMTEXT,
  target_json MEDIUMTEXT NOT NULL,
  reason VARCHAR(2000) NOT NULL,
  submitted_by BIGINT NOT NULL,
  submitted_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_employee_revision_request FOREIGN KEY (request_id) REFERENCES employee_request(id),
  CONSTRAINT uq_employee_revision UNIQUE (request_id, revision_number)
);

CREATE TABLE employee_request_activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id BIGINT NOT NULL,
  revision_id BIGINT,
  action VARCHAR(30) NOT NULL,
  actor_id BIGINT NOT NULL,
  reason VARCHAR(2000),
  occurred_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_employee_activity_request FOREIGN KEY (request_id) REFERENCES employee_request(id)
);

CREATE INDEX ix_employee_request_list ON employee_request(deleted, state, id);
CREATE INDEX ix_employee_request_subject ON employee_request(subject_id);
CREATE INDEX ix_employee_request_activity ON employee_request_activity(request_id, id);
