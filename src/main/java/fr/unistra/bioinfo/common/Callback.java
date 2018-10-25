package fr.unistra.bioinfo.common;

@FunctionalInterface
public interface Callback<T> {
    void call(T result);
}
