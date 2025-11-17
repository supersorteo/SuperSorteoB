package com.example.rifa.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.*;
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

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl; // De application.properties



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
                   // String fileUrl = "https://sweet-laughter-production.up.railway.app/api/images/" + fileName;
                    String fileUrl = baseUrl + "/api/images/" + fileName;
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
                       // String fileUrl = "https://sweet-laughter-production.up.railway.app/api/images/" + file.getName();
                        String fileUrl = baseUrl + "/api/images/" + file.getName();
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


    @GetMapping("/volume-info")
    public ResponseEntity<Map<String, Object>> getVolumeInfo() {
        File uploadDir = new File(UPLOAD_DIR);
        Map<String, Object> info = new HashMap<>();

        info.put("volumePath", UPLOAD_DIR);
        info.put("exists", uploadDir.exists());
        info.put("canWrite", uploadDir.canWrite());
        info.put("totalFiles", uploadDir.listFiles() != null ? uploadDir.listFiles().length : 0);

        long size = 0;
        if (uploadDir.listFiles() != null) {
            for (File file : uploadDir.listFiles()) {
                if (file.isFile()) size += file.length();
            }
        }
        info.put("totalSizeMB", size / (1024.0 * 1024.0));

        return ResponseEntity.ok(info);
    }


    @DeleteMapping("/cleanup/{rifaId}")
    public ResponseEntity<Map<String, Object>> cleanupRifaImages(@PathVariable Long rifaId,
                                                                 @RequestBody List<String> imageUrls) {
        int deletedCount = 0;
        List<String> errors = new ArrayList<>();

        for (String imageUrl : imageUrls) {
            try {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

                if (fileName.contains("..") || fileName.contains("/")) {
                    errors.add("Nombre inválido: " + fileName);
                    continue;
                }

                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                if (Files.deleteIfExists(filePath)) {
                    deletedCount++;
                    System.out.println("Imagen eliminada del volumen: " + fileName);
                }
            } catch (IOException e) {
                errors.add("Error eliminando: " + imageUrl);
                System.err.println("Error: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "deleted", deletedCount,
                "errors", errors
        ));
    }

   /* @DeleteMapping("/{fileName}")
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
*/

    @DeleteMapping("/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable("fileName") String fileName) {
        try {
            // Validación de nombre de archivo
            if (fileName.contains("..") || fileName.contains("/") || fileName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nombre de archivo inválido"));
            }

            // Ruta completa del archivo
            Path filePath = Paths.get("/app/uploads/" + fileName);
            File file = filePath.toFile();

            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Imagen no encontrada"));
            }

            // Eliminar el archivo
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("Imagen eliminada del volumen: " + fileName); // Log para depuración
                return ResponseEntity.ok(Map.of("message", "Imagen eliminada con éxito"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo eliminar la imagen"));
            }
        } catch (Exception e) {
            System.err.println("Error al eliminar imagen " + fileName + ": " + e.getMessage()); // Log de error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar la imagen", "details", e.getMessage()));
        }
    }

}
