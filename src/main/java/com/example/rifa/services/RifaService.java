package com.example.rifa.services;

import com.example.rifa.dto.RifaGanadorDTO;
import com.example.rifa.entity.CodigoVip;
import com.example.rifa.entity.Participante;
import com.example.rifa.entity.Rifa;
import com.example.rifa.entity.Usuario;
import com.example.rifa.exception.ResourceNotFoundException;
import com.example.rifa.repository.CodigoVipRepository;
import com.example.rifa.repository.ParticipanteRepository;
import com.example.rifa.repository.RifaRepository;
import com.example.rifa.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import com.example.rifa.services.WebSocketService;
@Service
public class RifaService {
    private final RifaRepository rifaRepository;
    private final UsuarioRepository usuarioRepository;
    private final WebSocketService websocketService;
    //private final RaffleImageGenerator imageGenerator;
    //private final String imageDirectory = "src/main/resources/static/images/";
    @Autowired
    private CodigoVipRepository codigoVipRepository;
    @Autowired
    public RifaService(RifaRepository rifaRepository, UsuarioRepository usuarioRepository, WebSocketService websocketService) {
        this.rifaRepository = rifaRepository;
        this.usuarioRepository = usuarioRepository;


        this.websocketService = websocketService;
    }


    @Autowired
    private ParticipanteRepository participanteRepository;




    public Rifa crearRifa(Rifa rifa, String codigoVip) {
        // Verificar si el usuario existe
        Usuario usuario = usuarioRepository.findById(rifa.getUsuario().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + rifa.getUsuario().getId()));

        // Manejo del código VIP (tu lógica actual)
        if (codigoVip != null) {
            if (!usuario.isEsVip()) {
                CodigoVip codigo = codigoVipRepository.findByCodigo(codigoVip)
                        .orElseThrow(() -> new IllegalArgumentException("Código VIP no válido."));
                if (codigo.isUtilizado()) {
                    throw new IllegalArgumentException("El código VIP ya fue utilizado.");
                }
                usuario.setEsVip(true);
                usuario.setCodigoVip(codigoVip);
                usuario.setCantidadRifas(codigo.getCantidadRifas());
                usuarioRepository.save(usuario);
                codigo.setUtilizado(true);
                codigoVipRepository.save(codigo);
            } else if (!usuario.getCodigoVip().equals(codigoVip)) {
                throw new IllegalArgumentException("El código VIP no corresponde al usuario.");
            }
        }

        // Genera automáticamente el código de la rifa si no se proporcionó uno
        if (rifa.getCode() == null || rifa.getCode().trim().isEmpty()) {
            rifa.setCode(generateRaffleCode());
        }

        // Verificar límites de rifas, etc. (tu lógica actual)
        int limiteRifas = usuario.isEsVip() ?
                codigoVipRepository.findByCodigo(usuario.getCodigoVip())
                        .map(CodigoVip::getCantidadRifas)
                        .orElse(Integer.MAX_VALUE)
                : 1;
        long rifasCreadas = usuario.isEsVip() ? rifaRepository.countByUsuario(usuario)
                : rifaRepository.countByUsuarioAndFechaSorteoBetween(usuario, LocalDate.now().withDayOfMonth(1), LocalDate.now().plusMonths(1).minusDays(1));
        if (rifasCreadas >= limiteRifas) {
            throw new IllegalArgumentException("Has alcanzado el límite de rifas permitidas.");
        }

        // Asignar el usuario y activar la rifa
        rifa.setUsuario(usuario);
        rifa.setActive(true);
        return rifaRepository.save(rifa);
    }




    // Método auxiliar para generar un código único para la rifa
    private String generateRaffleCode() {
        // Por ejemplo, "R-" seguido de 4 caracteres del UUID
        return "R-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    public Rifa obtenerRifaPorId(Long id) {
        return rifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + id));
    }

    public List<Rifa> obtenerTodasLasRifas() {
        return rifaRepository.findAll();
    }

    public Rifa actualizarRifa(Long id, Rifa rifaActualizada) {
        Rifa rifaExistente = rifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + id));

        rifaExistente.setNombre(rifaActualizada.getNombre());
        rifaExistente.setCantidadParticipantes(rifaActualizada.getCantidadParticipantes());
        rifaExistente.setFechaSorteo(rifaActualizada.getFechaSorteo());
        rifaExistente.setUsuario(rifaActualizada.getUsuario());
        rifaExistente.setProducto(rifaActualizada.getProducto());
        rifaExistente.setActive(rifaActualizada.isActive());
        rifaExistente.setPrecio(rifaActualizada.getPrecio());
        return rifaRepository.save(rifaExistente);
    }


    public void eliminarRifa(Long id) {
        // Buscar la rifa por ID
        Rifa rifa = rifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + id));

        // Eliminar todos los participantes asociados a esta rifa
        List<Participante> participantes = participanteRepository.findByRaffleId(id);
        if (!participantes.isEmpty()) {
            participanteRepository.deleteAll(participantes);
        }

        // Eliminar la rifa (esto también eliminará el producto asociado si está configurado con CascadeType.ALL)
        rifaRepository.delete(rifa);
    }

    // Obtener todas las rifas de un usuario por su ID
    public List<Rifa> obtenerRifasPorUsuarioId(Long usuarioId) {
        // Verificar si el usuario existe (opcional, dependiendo de tu lógica)
        // usuarioRepository.findById(usuarioId).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // Obtener las rifas del usuario
        List<Rifa> rifas = rifaRepository.findByUsuarioId(usuarioId);
        if (rifas.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron rifas para el usuario con ID: " + usuarioId);
        }
        return rifas;
    }

    public List<RifaGanadorDTO> getAllWinners() {
        List<Rifa> rifasEjecutadas = rifaRepository.findByExecutedTrue();

        return rifasEjecutadas.stream()
                .map(rifa -> {
                    Participante ganador = participanteRepository.findByRaffleId(rifa.getId()).stream()
                            .filter(p -> p.getReservedNumber() != null && p.getReservedNumber().equals(rifa.getWinningNumber()))
                            .findFirst()
                            .orElse(null);

                    return new RifaGanadorDTO(rifa, ganador, participanteRepository.findByRaffleId(rifa.getId()));
                })
                .toList();
    }



    public RifaGanadorDTO getWinnerByRaffleId(Long rifaId) {
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + rifaId));

        Participante ganador = participanteRepository.findByRaffleId(rifaId).stream()
                .filter(p -> p.getReservedNumber() != null && p.getReservedNumber().equals(rifa.getWinningNumber()))
                .findFirst()
                .orElse(null);

        return new RifaGanadorDTO(rifa, ganador, participanteRepository.findByRaffleId(rifaId));
    }




/*
    public RifaGanadorDTO ejecutarSorteo1(Long rifaId) {
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + rifaId));

        if (!rifa.isActive()) {
            throw new IllegalArgumentException("La rifa ya ha sido finalizada.");
        }

        List<Participante> participantes = participanteRepository.findByRaffleId(rifaId);

        if (participantes.isEmpty()) {
            throw new IllegalArgumentException("No hay participantes en esta rifa.");
        }

        int totalParticipantes = rifa.getCantidadParticipantes();
        if (totalParticipantes <= 0) {
            throw new IllegalArgumentException("La rifa tiene una cantidad inválida de participantes.");
        }

        List<Integer> numerosReservados = participantes.stream()
                .map(Participante::getReservedNumber)
                .filter(Objects::nonNull)
                .toList();

        System.out.println("🔢 Números reservados en esta rifa: " + numerosReservados);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error en el temporizador de ejecución.");
        }

        Random random = new Random();
        int winningNumber = random.nextInt(totalParticipantes) + 1;

        System.out.println("🎯 Número ganador generado: " + winningNumber);

        Participante ganador = participantes.stream()
                .filter(p -> p.getReservedNumber() != null && p.getReservedNumber().equals(winningNumber))
                .findFirst()
                .orElse(null);

        rifa.setWinningNumber(winningNumber);
        rifaRepository.save(rifa);

        if (ganador == null) {
            System.out.println("🚨 El número ganador NO está reservado. La rifa sigue activa.");
            return new RifaGanadorDTO(rifa, null, participantes);
        }

        rifa.setExecuted(true);
        rifa.setActive(false);
        rifaRepository.save(rifa);

        System.out.println("🏆 Número ganador definitivo: " + winningNumber);
        System.out.println("🎉 Datos del ganador: " + ganador.getName() + " " + ganador.getLastName() + ", Teléfono: " + ganador.getPhone());

        return new RifaGanadorDTO(rifa, ganador, participantes);
    }*/


   /* public CompletableFuture<RifaGanadorDTO> ejecutarSorteo(Long rifaId) {
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + rifaId));

        if (!rifa.isActive()) {
            throw new IllegalArgumentException("La rifa ya ha sido finalizada.");
        }

        List<Participante> participantes = participanteRepository.findByRaffleId(rifaId);
        if (participantes.isEmpty()) {
            throw new IllegalArgumentException("No hay participantes en esta rifa.");
        }

        int totalParticipantes = rifa.getCantidadParticipantes();
        if (totalParticipantes <= 0) {
            throw new IllegalArgumentException("La rifa tiene una cantidad inválida de participantes.");
        }

        List<Integer> numerosReservados = participantes.stream()
                .map(Participante::getReservedNumber)
                .filter(Objects::nonNull)
                .toList();

        System.out.println("🔢 Números reservados en esta rifa: " + numerosReservados);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error en el temporizador de ejecución.");
        }

        Random random = new Random();
        int winningNumber = random.nextInt(totalParticipantes) + 1;
        System.out.println("🎯 Número ganador generado: " + winningNumber);

        Participante ganador = participantes.stream()
                .filter(p -> p.getReservedNumber() != null && p.getReservedNumber().equals(winningNumber))
                .findFirst()
                .orElse(null);

        rifa.setWinningNumber(winningNumber);
        rifaRepository.save(rifa);

        if (ganador == null) {
            System.out.println("🚨 El número ganador NO está reservado. La rifa sigue activa.");
            websocketService.sendWinnerUpdate(rifaId, null, winningNumber, rifa);
            return CompletableFuture.completedFuture(new RifaGanadorDTO(rifa, null, participantes));
        }

        rifa.setExecuted(true);
        rifa.setActive(false);
        rifaRepository.save(rifa);

        System.out.println("🏆 Número ganador definitivo: " + winningNumber);
        System.out.println("🎉 Datos del ganador: " + ganador.getName() + " " + ganador.getLastName() + ", Teléfono: " + ganador.getPhone());

        websocketService.sendWinnerUpdate(rifaId, ganador, winningNumber, rifa);



        return CompletableFuture.completedFuture(new RifaGanadorDTO(rifa, ganador, participantes));
    }*/


    public CompletableFuture<RifaGanadorDTO> ejecutarSorteo(Long rifaId) {
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + rifaId));

        if (!rifa.isActive()) {
            throw new IllegalArgumentException("La rifa ya ha sido finalizada.");
        }

        List<Participante> participantes = participanteRepository.findByRaffleId(rifaId);
        if (participantes.isEmpty()) {
            throw new IllegalArgumentException("No hay participantes en esta rifa.");
        }

        int totalParticipantes = rifa.getCantidadParticipantes();
        if (totalParticipantes <= 0) {
            throw new IllegalArgumentException("La rifa tiene una cantidad inválida de participantes.");
        }

        // 🔥 Enviar evento WebSocket para iniciar la ejecución del sorteo
        websocketService.sendRaffleExecutionStart(rifaId);

        List<Integer> numerosReservados = participantes.stream()
                .map(Participante::getReservedNumber)
                .filter(Objects::nonNull)
                .toList();

        System.out.println("🔢 Números reservados en esta rifa: " + numerosReservados);

        try {
            for (int i = 5; i >= 1; i--) {  // ⏳ Notificar el conteo regresivo
                websocketService.sendCountdownUpdate(rifaId, i);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error en el temporizador de ejecución.");
        }

        // 🔥 Generar número ganador
        int winningNumber = new Random().nextInt(totalParticipantes) + 1;
        System.out.println("🎯 Número ganador generado: " + winningNumber);

        Participante ganador = participantes.stream()
                .filter(p -> p.getReservedNumber() != null && p.getReservedNumber().equals(winningNumber))
                .findFirst()
                .orElse(null);

        // 🔄 Actualizar estado de la rifa
        rifa.setWinningNumber(winningNumber);
        rifaRepository.save(rifa);

        if (ganador == null) {
            System.out.println("🚨 El número ganador NO está reservado. La rifa sigue activa.");
            websocketService.sendWinnerUpdate(rifaId, null, winningNumber);
            return CompletableFuture.completedFuture(new RifaGanadorDTO(rifa, null, participantes));
        }

        // 🔥 Solo si el número ganador está en `numerosReservados`, la rifa se marca como finalizada
        if (numerosReservados.contains(winningNumber)) {
            rifa.setExecuted(true);
            rifa.setActive(false);
            rifaRepository.save(rifa);
        }

        System.out.println("🏆 Número ganador definitivo: " + winningNumber);
        System.out.println("🎉 Datos del ganador: " + ganador.getName() + " " + ganador.getLastName() + ", Teléfono: " + ganador.getPhone());

        // 🔥 Notificar el resultado del sorteo en WebSockets
        //websocketService.sendWinnerUpdate(rifaId, ganador, winningNumber, rifa);
        // 🔥 Notificar el resultado del sorteo en WebSockets correctamente
        websocketService.sendWinnerUpdate(rifaId, ganador, winningNumber);


        return CompletableFuture.completedFuture(new RifaGanadorDTO(rifa, ganador, participantes));
    }









}
