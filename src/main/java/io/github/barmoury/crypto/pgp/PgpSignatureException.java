package io.github.barmoury.crypto.pgp;

public class PgpSignatureException extends RuntimeException {

    public PgpSignatureException(String message) {
        super(message);
    }

    public PgpSignatureException(String message, Exception exception) {
        super(message, exception);
    }

}
