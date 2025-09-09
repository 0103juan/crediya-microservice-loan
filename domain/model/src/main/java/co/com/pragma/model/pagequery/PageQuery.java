package co.com.pragma.model.pagequery;

public record PageQuery(int page, int size) {
    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser positivo.");
        }
    }
}