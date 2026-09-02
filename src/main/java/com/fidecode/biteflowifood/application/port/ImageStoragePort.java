package com.fidecode.biteflowifood.application.port;

public interface ImageStoragePort {
    /**
     * Faz o download da imagem a partir de uma URL e armazena.
     * Retorna o caminho ou URL local que o sistema passará a usar.
     */
    String storeImageFromUrl(String imageUrl);
}
