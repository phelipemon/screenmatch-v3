package br.com.alura.screenmatch_v300.service;

public interface IConverteDados {

    <T> T obterDados(String json, Class<T> classe);
}
