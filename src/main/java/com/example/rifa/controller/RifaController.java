package com.example.rifa.controller;

import com.example.rifa.dto.CrearRifaRequest;
import com.example.rifa.dto.RifaGanadorDTO;
import com.example.rifa.entity.Rifa;
import com.example.rifa.exception.ResourceNotFoundException;
import com.example.rifa.services.RifaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/rifas")
//@CrossOrigin(origins = {"http://localhost:4200", "http://otrodominio.com"}) // 🔥 Especificar los orígenes explícitamente


public class RifaController {

    private final RifaService rifaService;

    public RifaController(RifaService rifaService) {
        this.rifaService = rifaService;
    }


    @PostMapping // Ruta específica para crear una rifa
    public ResponseEntity<?> crearRifa(
            @RequestBody Rifa rifa, // El cuerpo de la solicitud incluye las URLs de las imágenes
            @RequestParam(required = false) String codigoVip) {
        try {
            Rifa nuevaRifa = rifaService.crearRifa(rifa, codigoVip);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRifa);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<Rifa> obtenerRifaPorId(@PathVariable Long id) {
        Rifa rifa = rifaService.obtenerRifaPorId(id);
        return ResponseEntity.ok(rifa);
    }

    @GetMapping
    public ResponseEntity<List<Rifa>> obtenerTodasLasRifas() {
        List<Rifa> rifas = rifaService.obtenerTodasLasRifas();
        return ResponseEntity.ok(rifas);
    }




    @PutMapping("/{id}")
    public ResponseEntity<Rifa> actualizarRifa(@PathVariable Long id, @RequestBody Rifa rifaActualizada) {
        Rifa rifa = rifaService.actualizarRifa(id, rifaActualizada);
        return ResponseEntity.ok(rifa);
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarRifa(@PathVariable Long id) {
        try {
            rifaService.eliminarRifa(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            // Puedes devolver 409 Conflict o el error que consideres adecuado
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    // Endpoint para obtener rifas por ID de usuario
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Rifa>> obtenerRifasPorUsuarioId(@PathVariable Long usuarioId) {
        List<Rifa> rifas = rifaService.obtenerRifasPorUsuarioId(usuarioId);
        return ResponseEntity.ok(rifas);
    }


    @GetMapping("/winners")
    public ResponseEntity<List<RifaGanadorDTO>> getAllWinners() {
        return ResponseEntity.ok(rifaService.getAllWinners());
    }

    @GetMapping("/winners/{rifaId}")
    public ResponseEntity<RifaGanadorDTO> getWinnerByRaffleId(@PathVariable Long rifaId) {
        return ResponseEntity.ok(rifaService.getWinnerByRaffleId(rifaId));
    }


/*
    @PutMapping("/execute/{id}")
    public ResponseEntity<RifaGanadorDTO> ejecutarSorteo(@PathVariable Long id) {
        return ResponseEntity.ok(rifaService.ejecutarSorteo(id));
    }*/



    @PutMapping("/execute/{id}")
    public CompletableFuture<ResponseEntity<RifaGanadorDTO>> ejecutarSorteo(@PathVariable Long id) {
        return rifaService.ejecutarSorteo(id)
                .thenApply(resultado -> ResponseEntity.ok(resultado));
    }






}
