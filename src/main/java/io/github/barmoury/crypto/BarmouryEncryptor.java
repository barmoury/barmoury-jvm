package io.github.barmoury.crypto;

public interface BarmouryEncryptor<T> {

    String encrypt(T t);
    T decrypt(String encrypted);

}
