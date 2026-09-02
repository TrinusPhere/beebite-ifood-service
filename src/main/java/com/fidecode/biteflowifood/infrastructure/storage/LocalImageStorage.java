package com.fidecode.biteflowifood.infrastructure.storage;

import com.fidecode.biteflowifood.application.port.ImageStoragePort;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@ApplicationScoped
public class LocalImageStorage implements ImageStoragePort {

    // Em produção, isso seria configurado em S3 ou diretório específico.
    // Para MVP, salvamos num diretório local do sistema.
    private static final String UPLOAD_DIR = "uploads/images";

    @Override
    public String storeImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = ".png"; // Default
            if (imageUrl.contains(".jpg")) extension = ".jpg";
            if (imageUrl.contains(".jpeg")) extension = ".jpeg";

            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(fileName);

            try (InputStream in = URI.create(imageUrl).toURL().openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Retorna o path relativo ou uma URL estática
            // Como é um MVP, apenas retornamos a URL do arquivo ou o nome do arquivo,
            // que seria servido pelo backend em um endpoint estático.
            return "/api/images/" + fileName; 
        } catch (Exception e) {
            // Em caso de erro ao baixar a imagem, podemos apenas logar e retornar null ou a original.
            // Aqui decidimos lançar exception para evitar produtos sem imagem se for requerido,
            // mas o iFood às vezes tem URLs de imagens que falham. Vamos retornar a original como fallback.
            return imageUrl;
        }
    }
}
