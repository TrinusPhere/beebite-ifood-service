package com.fidecode.biteflowifood.infrastructure.storage;

import com.fidecode.biteflowifood.application.port.ImageStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class LocalImageStorage implements ImageStoragePort {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    @ConfigProperty(name = "biteflow.storage.images.dir", defaultValue = "uploads/images")
    String uploadDir = "uploads/images";

    @ConfigProperty(name = "biteflow.storage.images.public-url", defaultValue = "/api/images")
    String publicBaseUrl = "/api/images";

    @Override
    public String storeImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        URI uri;
        try {
            uri = URI.create(imageUrl.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            return null;
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String lowerPath = uri.getPath() != null ? uri.getPath().toLowerCase(Locale.ROOT) : "";
            String extension = ".png";
            if (lowerPath.endsWith(".jpg") || imageUrl.contains(".jpg")) {
                extension = ".jpg";
            } else if (lowerPath.endsWith(".jpeg") || imageUrl.contains(".jpeg")) {
                extension = ".jpeg";
            } else if (lowerPath.endsWith(".webp") || imageUrl.contains(".webp")) {
                extension = ".webp";
            }

            String fileName = UUID.randomUUID() + extension;
            Path filePath = uploadPath.resolve(fileName).normalize();
            if (!filePath.startsWith(uploadPath)) {
                return null;
            }

            URLConnection connection = uri.toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            String baseUrl = (publicBaseUrl != null && !publicBaseUrl.isBlank())
                    ? publicBaseUrl.replaceAll("/+$", "")
                    : "/api/images";
            return baseUrl + "/" + fileName;
        } catch (Exception e) {
            return imageUrl;
        }
    }
}
