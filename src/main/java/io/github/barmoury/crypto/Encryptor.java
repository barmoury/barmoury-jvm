package io.github.barmoury.crypto;

public interface Encryptor<T> {

    String encrypt(T t);
    T decrypt(String encrypted);

}
