package com.example.rifa.controller;

import com.example.rifa.entity.Participante;
import com.example.rifa.services.ParticipanteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participantes")
//@CrossOrigin(origins = {"http://localhost:4200", "http://otrodominio.com"}) // 🔥 Especificar los orígenes explícitamente


public class ParticipanteController {

    @Autowired
    private ParticipanteService participanteService;

    // GET: Obtener todos los participantes
    @GetMapping
    public ResponseEntity<List<Participante>> getAllParticipantes() {
        return ResponseEntity.ok(participanteService.getAllParticipantes());
    }

    // GET: Obtener un participante por ID
    @GetMapping("/{id}")
    public ResponseEntity<Participante> getParticipanteById(@PathVariable Long id) {
        return ResponseEntity.ok(participanteService.getParticipanteById(id));
    }

    // GET: Obtener participantes por el id de la rifa
    @GetMapping("/raffle/{raffleId}")
    public ResponseEntity<List<Participante>> getParticipantesByRaffleId(@PathVariable Long raffleId) {
        return ResponseEntity.ok(participanteService.getParticipantesByRaffleId(raffleId));
    }



    // POST: Crear un nuevo participante
    @PostMapping
    public ResponseEntity<Participante> createParticipante(@RequestBody Participante participante) {
        return ResponseEntity.ok(participanteService.createParticipante(participante));
    }


    // PUT: Actualizar un participante
    @PutMapping("/{id}")
    public ResponseEntity<Participante> updateParticipante(@PathVariable Long id, @RequestBody Participante participanteDetails) {
        return ResponseEntity.ok(participanteService.updateParticipante(id, participanteDetails));
    }

    // DELETE: Eliminar un participante
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipante(@PathVariable Long id) {
        participanteService.deleteParticipante(id);
        return ResponseEntity.noContent().build();
    }


    // GET: Ejecutar el sorteo y devolver al ganador
    @GetMapping("/raffle/{raffleId}/sorteo")
    public ResponseEntity<Participante> sortearGanador(@PathVariable Long raffleId) {
        return ResponseEntity.ok(participanteService.sortearGanador(raffleId));
    }


}
