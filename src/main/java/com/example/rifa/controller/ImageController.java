package com.example.rifa.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/images")
//@CrossOrigin(origins = {"http://localhost:4200", "http://otrodominio.com"}) // 🔥 Especificar los orígenes explícitamente


public class ImageController {
    //private static final String UPLOAD_DIR = "src/main/resources/uploads/";
   // private static final String UPLOAD_DIR = "uploads/";
    private static final String UPLOAD_DIR = "/app/uploads/";



    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        try {
            // Verificar y crear el directorio /uploads si no existe
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            System.out.println("Archivos recibidos: " + files.size());
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    Path filePath = Paths.get(UPLOAD_DIR + fileName);
                    Files.write(filePath, file.getBytes());
                    String fileUrl = "http://localhost:8080/api/images/" + fileName;
                    urls.add(fileUrl);
                    System.out.println("Archivo guardado: " + fileName);
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("urls", urls);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al subir las imágenes", "details", e.getMessage()));
        }
    }



    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> getImage(@PathVariable("fileName") String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            File file = filePath.toFile();

            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            byte[] imageBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", contentType);

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<String>> getAllImages() {
        try {
            File folder = new File(UPLOAD_DIR);
            File[] files = folder.listFiles();

            if (files != null) {
                List<String> fileNames = new ArrayList<>();
                for (File file : files) {
                    if (file.isFile()) {
                        // Agregar la URL completa de la imagen
                        String fileUrl = "http://localhost:8080/api/images/" + file.getName();
                        fileNames.add(fileUrl);
                    }
                }
                return ResponseEntity.ok(fileNames);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonList("No se pudieron obtener las imágenes"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList("Error al obtener las imágenes"));
        }
    }

    @DeleteMapping("/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable("fileName") String fileName) {
        try {
            // Ruta completa del archivo a eliminar
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            File file = filePath.toFile();

            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Imagen no encontrada"));
            }

            // Eliminar el archivo
            boolean deleted = file.delete();
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Imagen eliminada con éxito"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "No se pudo eliminar la imagen"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar la imagen", "details", e.getMessage()));
        }
    }



}
