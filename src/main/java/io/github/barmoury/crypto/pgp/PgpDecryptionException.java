package io.github.barmoury.crypto.pgp;

public class PgpDecryptionException extends RuntimeException {

    public PgpDecryptionException(String message) {
        super(message);
    }

    public PgpDecryptionException(String message, Exception exception) {
        super(message, exception);
    }

}
