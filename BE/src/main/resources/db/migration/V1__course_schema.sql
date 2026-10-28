CREATE TABLE tbl_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    date_of_birth DATE,
    gender VARCHAR(30),
    phone VARCHAR(30),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tbl_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address VARCHAR(500) NOT NULL,
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES tbl_user(id)
);

CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(150) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(60) NOT NULL,
    level VARCHAR(60) NOT NULL,
    instructor VARCHAR(100) NOT NULL,
    instructor_title VARCHAR(200) NOT NULL,
    price DECIMAL(12,0) NOT NULL,
    original_price DECIMAL(12,0) NOT NULL,
    hours DECIMAL(5,1) NOT NULL,
    rating DECIMAL(2,1) NOT NULL,
    student_count INT NOT NULL,
    cover_key VARCHAR(30) NOT NULL,
    badge VARCHAR(50),
    outcomes TEXT NOT NULL
);

CREATE TABLE lesson (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    chapter VARCHAR(150) NOT NULL,
    position INT NOT NULL,
    duration_minutes INT NOT NULL,
    video_url VARCHAR(1000) NOT NULL,
    preview BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_lesson_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT uq_lesson_position UNIQUE (course_id, position)
);

CREATE TABLE course_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    amount DECIMAL(12,0) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES tbl_user(id),
    CONSTRAINT fk_order_course FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT uq_order_course UNIQUE (user_id, course_id)
);

CREATE TABLE lesson_progress (
    user_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, lesson_id),
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES tbl_user(id),
    CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(id)
);
