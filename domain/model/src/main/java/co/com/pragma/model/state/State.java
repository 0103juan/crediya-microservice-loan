package co.com.pragma.model.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum State {

    REVIEW_PENDING(1, "Préstamo pendiente de revisión"),
    APPROVED(2, "Préstamo Aprobado"),
    REJECTED(3, "Préstamo Rechazado"),
    MANUAL_REVIEW(4, "Préstamo en revisión manual");

    private final Integer id;
    private final String description;

    public static State of(int id) {
        return Stream.of(State.values())
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ID de rol inválido: " + id));
    }
}