package tech.muiru.ncba.exception;

public record CustomExceptionDto(
        Integer statusCode,
        String message,
        String error,
        String path
) {
}
