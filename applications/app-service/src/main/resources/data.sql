-- Inserción de los estados de préstamo
INSERT INTO state (id, name, description) VALUES
(1, 'REVIEW_PENDING', 'Préstamo pendiente de revisión'),
(2, 'APPROVED', 'Préstamo Aprobado'),
(3, 'REJECTED', 'Préstamo Rechazado'),
(4, 'MANUAL_REVIEW', 'Préstamo en revisión manual')
ON CONFLICT (id) DO NOTHING;

-- Inserción de los tipos de préstamo
INSERT INTO loan_types (id, name, min_amount, max_amount, interest_rate, automatic_validation) VALUES
(1, 'Crédito de Libre Inversión', 1000000.00, 80000000.00, 1.80, TRUE),
(2, 'Crédito Vehicular', 15000000.00, 120000000.00, 1.50, FALSE),
(3, 'Crédito Hipotecario', 80000000.00, 900000000.00, 1.10, FALSE),
(4, 'Adelanto de Nómina', 100000.00, 2500000.00, 2.20, TRUE),
(5, 'Crédito Educativo Posgrado', 3000000.00, 60000000.00, 0.90, FALSE)
ON CONFLICT (id) DO NOTHING;