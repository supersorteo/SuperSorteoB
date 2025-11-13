package com.example.rifa.controller;

import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.io.FileInputStream;
import org.springframework.http.HttpHeaders;

import java.nio.file.Paths;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api/videos")
//@CrossOrigin(origins = {"http://localhost:4200", "http://otrodominio.com"}) // 🔥 Especificar los orígenes explícitamente


public class VideoController {

    //private static final String VIDEO_DIRECTORY = "D:/uploadVideos/";
    private static final String VIDEO_DIRECTORY = "/app/videos/";

    // Lista de extensiones permitidas (puedes agregar más si es necesario)
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("mp4", "mpg", "avi", "mkv", "mov", "wmv", "flv", "webm");

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @GetMapping("/list")
    public ResponseEntity<List<String>> listVideos() {
        File folder = new File(VIDEO_DIRECTORY);
        List<String> videoNames = new ArrayList<>();

        if (!folder.exists() || !folder.isDirectory()) {
            logger.warn("La carpeta de videos no existe: {}", VIDEO_DIRECTORY);
            return ResponseEntity.ok(videoNames);
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                // Extraer la extensión en minúsculas
                String name = file.getName();
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex > 0) {
                    String ext = name.substring(dotIndex + 1).toLowerCase();
                    if (ALLOWED_EXTENSIONS.contains(ext)) {
                        videoNames.add(name);
                    }
                }
            }
        }
        logger.info("Videos encontrados: {}", videoNames);
        return ResponseEntity.ok(videoNames);
    }


    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadVideo(@RequestParam("file") MultipartFile file) {
        try {
            // Verificar y crear la carpeta de videos si no existe
            File uploadDir = new File(VIDEO_DIRECTORY);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                logger.info("Carpeta creada: {}", VIDEO_DIRECTORY);
            }

            // Validar extensión
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.badRequest().body("Nombre de archivo inválido.");
            }
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex < 0) {
                return ResponseEntity.badRequest().body("El archivo no tiene extensión.");
            }
            String ext = originalFilename.substring(dotIndex + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                return ResponseEntity.badRequest().body("Formato de video no permitido: " + ext);
            }

            // Verificar si el archivo ya existe en la base de datos
            Path filePath = Paths.get(VIDEO_DIRECTORY).resolve(originalFilename).normalize();
            if (Files.exists(filePath)) {
                logger.warn("Intento de subir un video duplicado: {}", originalFilename);
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Este video ya está en la base de datos.");
            }

            // Guardar el archivo si no existe
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Video subido correctamente: {}", originalFilename);
            return ResponseEntity.ok("Video subido correctamente: " + originalFilename);
        } catch (IOException e) {
            logger.error("Error al subir el video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al subir el video: " + e.getMessage());
        }
    }






    @GetMapping("/{videoName}")
    public ResponseEntity<Resource> getVideo(@PathVariable String videoName) {
        try {
            // Validar que el nombre no contenga caracteres ilegales
            if (videoName.contains("<") || videoName.contains(">")) {
                logger.error("Nombre de archivo inválido: {}", videoName);
                return ResponseEntity.badRequest().body(null);
            }

            Path filePath = Paths.get(VIDEO_DIRECTORY).resolve(videoName).normalize();
            logger.info("Buscando archivo en: {}", filePath.toString());

            Resource resource = new UrlResource(filePath.toUri());
            logger.info("Existe? {} - Legible? {}", resource.exists(), resource.isReadable());

            if (resource.exists() && resource.isReadable()) {
                // Detectar el tipo de contenido automáticamente
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                logger.info("Archivo encontrado: {}. Content-Type: {}", resource.getFilename(), contentType);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .body(resource);
            } else {
                logger.error("Archivo no encontrado o ilegible: {}", videoName);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error al obtener el video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }





    @DeleteMapping("/{videoName}")
    public ResponseEntity<String> deleteVideo(@PathVariable String videoName) {
        try {
            // Validar que el nombre no contenga caracteres ilegales
            if (videoName.contains("<") || videoName.contains(">")) {
                logger.error("Nombre de archivo inválido: {}", videoName);
                return ResponseEntity.badRequest().body("Nombre de archivo inválido.");
            }

            // Construir la ruta absoluta del archivo
            Path filePath = Paths.get(VIDEO_DIRECTORY).resolve(videoName).normalize();
            File file = filePath.toFile();

            // Verificar si el archivo existe
            if (!file.exists()) {
                logger.error("Archivo no encontrado: {}", videoName);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Archivo no encontrado.");
            }

            // Intentar eliminar el archivo
            if (file.delete()) {
                logger.info("Archivo eliminado correctamente: {}", videoName);
                return ResponseEntity.ok("Archivo eliminado correctamente: " + videoName);
            } else {
                logger.error("No se pudo eliminar el archivo: {}", videoName);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se pudo eliminar el archivo.");
            }
        } catch (Exception e) {
            logger.error("Error al eliminar el video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar el video: " + e.getMessage());
        }
    }


}

