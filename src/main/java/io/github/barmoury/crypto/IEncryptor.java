package io.github.barmoury.crypto;

public interface IEncryptor<T> {

    String encrypt(T t);
    T decrypt(String encrypted);

}
