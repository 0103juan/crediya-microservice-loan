-- Creación de la tabla de estados de préstamo
CREATE TABLE IF NOT EXISTS state (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- Creación de la tabla de tipos de préstamo
CREATE TABLE IF NOT EXISTS loan_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    min_amount NUMERIC(12, 2) NOT NULL,
    max_amount NUMERIC(12, 2) NOT NULL,
    interest_rate NUMERIC(5, 2) NOT NULL,
    automatic_validation BOOLEAN NOT NULL
);

-- Creación de la tabla de préstamos con llaves foráneas
CREATE TABLE IF NOT EXISTS loans (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(12, 2) NOT NULL,
    term INTEGER NOT NULL,
    user_email VARCHAR(100) NOT NULL,
    user_id_number VARCHAR(15) NOT NULL,
    id_loan_type INTEGER REFERENCES loan_types(id),
    id_state INTEGER REFERENCES state(id)
);