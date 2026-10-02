package com.lmt.fyp.flowerplus.module.product.service;

import com.lmt.fyp.flowerplus.common.ErrorCode;
import com.lmt.fyp.flowerplus.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
public class LocalProductImageStorage {

    private static final long MAX_IMAGE_BYTES = 2_000_000L;
    private static final byte[] PNG_SIGNATURE = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private final Path root;

    public LocalProductImageStorage(
            @Value("${flowerplus.product-image.upload-directory:./product-image-storage}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(UUID productId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.PRODUCT_IMAGE_INVALID, "An image file is required");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ApiException(ErrorCode.PRODUCT_IMAGE_TOO_LARGE, "Product images must not exceed 2,000,000 bytes");
        }

        try {
            byte[] bytes = file.getBytes();
            ImageFormat format = detectFormat(bytes);
            String fileName = UUID.randomUUID() + format.extension();
            Path productDirectory = root.resolve(productId.toString()).normalize();
            Path destination = productDirectory.resolve(fileName).normalize();
            if (!productDirectory.startsWith(root) || !destination.startsWith(root)) {
                throw new ApiException(ErrorCode.PRODUCT_IMAGE_INVALID, "Invalid image destination");
            }

            Files.createDirectories(productDirectory);
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return root.relativize(destination).toString().replace('\\', '/');
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not store the uploaded Product image");
        }
    }

    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Product image file was not found");
        }
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Could not remove the staged Product image");
        }
    }

    public String mediaType(String storageKey) {
        String key = storageKey.toLowerCase(java.util.Locale.ROOT);
        if (key.endsWith(".png")) {
            return "image/png";
        }
        if (key.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (key.endsWith(".webp")) {
            return "image/webp";
        }
        throw new ApiException(ErrorCode.NOT_FOUND, "Product image file was not found");
    }

    private Path resolve(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Product image file was not found");
        }
        return resolved;
    }

    private static ImageFormat detectFormat(byte[] bytes) {
        if (matches(bytes, PNG_SIGNATURE)) {
            return new ImageFormat(".png", "image/png");
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return new ImageFormat(".jpg", "image/jpeg");
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return new ImageFormat(".webp", "image/webp");
        }
        throw new ApiException(ErrorCode.PRODUCT_IMAGE_INVALID, "Only JPG, PNG, or WebP image files are accepted");
    }

    private static boolean matches(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private record ImageFormat(String extension, String mediaType) {
    }
}
