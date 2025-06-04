package com.example.rifa.dto;

import com.example.rifa.entity.Participante;
import com.example.rifa.entity.Rifa;

import java.util.List;

public class RifaGanadorDTO {

    private Rifa rifa;
    private Participante ganador;
    private List<Participante> participantes;

    public RifaGanadorDTO(Rifa rifa, Participante ganador, List<Participante> participantes) {
        this.rifa = rifa;
        this.ganador = ganador;
        this.participantes = participantes;
    }

    public Rifa getRifa() {
        return rifa;
    }

    public Participante getGanador() {
        return ganador;
    }

    public List<Participante> getParticipantes() { // 🔥 NUEVO
        return participantes;
    }
}

