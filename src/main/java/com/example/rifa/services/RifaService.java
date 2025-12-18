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
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.PreferenceBackUrls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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


    @Value("${mp.access.token}")


    private String mpAccessToken;




    public Rifa crearRifa0(Rifa rifa, String codigoVip) {
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

                usuario.setFechaRegistro(ZonedDateTime.now());

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

        // Verificar límites de rifas
        int limiteRifas = usuario.isEsVip() ?
                codigoVipRepository.findByCodigo(usuario.getCodigoVip())
                        .map(CodigoVip::getCantidadRifas)
                        .orElse(Integer.MAX_VALUE)
                : 1;
        long rifasCreadas = usuario.isEsVip() ? rifaRepository.countByUsuarioAndCodigoVipUsado(usuario, usuario.getCodigoVip())
                : rifaRepository.countByUsuarioAndFechaSorteoBetween(usuario, LocalDate.now().withDayOfMonth(1), LocalDate.now().plusMonths(1).minusDays(1));

        System.out.println("Debug: Usuario ID " + usuario.getId() + ", limiteRifas = " + limiteRifas + ", rifasCreadas (filtradas por código) = " + rifasCreadas);
        if (rifasCreadas >= limiteRifas) {
            throw new IllegalArgumentException("Has alcanzado el límite de rifas permitidas para este código VIP.");
        }

        // Asignar el usuario, active, y nuevo campo
        rifa.setUsuario(usuario);
        rifa.setActive(true);
        if (usuario.isEsVip()) {
            rifa.setCodigoVipUsado(usuario.getCodigoVip()); // Asocia a código actual
        }
        Rifa savedRifa = rifaRepository.saveAndFlush(rifa); // saveAndFlush fuerza persistencia
        System.out.println("Rifa guardada con codigoVipUsado = " + savedRifa.getCodigoVipUsado()); // Confirma

        // 🔥 Decrementar rifas si VIP
        if (usuario.isEsVip()) {
            this.decrementarCantidadRifas(usuario.getId());
            System.out.println("Rifas restantes para usuario " + usuario.getId() + ": " + usuario.getCantidadRifas());
        }

        return savedRifa;
    }

    public Rifa crearRifa(Rifa rifa, String codigoVip) {
        // Verificar si el usuario existe
        Usuario usuario = usuarioRepository.findById(rifa.getUsuario().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + rifa.getUsuario().getId()));

        // 🔥 VALIDACIÓN: Cuenta VIP expirada (más de 30 días desde fechaRegistro)
        if (usuario.isEsVip() && usuario.getFechaRegistro() != null) {
            ZonedDateTime fechaRegistro = usuario.getFechaRegistro();
            ZonedDateTime fechaExpiracion = fechaRegistro.plusDays(30);

            if (ZonedDateTime.now().isAfter(fechaExpiracion)) {
                // La cuenta ha expirado → desactivamos VIP automáticamente
                usuario.setEsVip(false);
                usuario.setCantidadRifas(0);
                usuario.setCodigoVip(null); // Opcional: limpiar código viejo
                usuarioRepository.save(usuario);

                throw new IllegalArgumentException("Tu cuenta VIP ha expirado (más de 30 días desde la activación). Compra un nuevo código VIP para seguir creando rifas.");
            }
        }

        // Manejo del código VIP (tu lógica actual para activación manual)
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
                usuario.setFechaRegistro(ZonedDateTime.now()); // Resetea el contador de 30 días
                usuarioRepository.save(usuario);
                codigo.setUtilizado(true);
                codigoVipRepository.save(codigo);
            } else if (!usuario.getCodigoVip().equals(codigoVip)) {
                throw new IllegalArgumentException("El código VIP no corresponde al usuario.");
            }
        }

        // Genera código de rifa si no existe
        if (rifa.getCode() == null || rifa.getCode().trim().isEmpty()) {
            rifa.setCode(generateRaffleCode());
        }

        // Verificar límites de rifas (tu lógica actual)
        int limiteRifas = usuario.isEsVip() ?
                codigoVipRepository.findByCodigo(usuario.getCodigoVip())
                        .map(CodigoVip::getCantidadRifas)
                        .orElse(Integer.MAX_VALUE)
                : 1;

        long rifasCreadas = usuario.isEsVip() ?
                rifaRepository.countByUsuarioAndCodigoVipUsado(usuario, usuario.getCodigoVip())
                : rifaRepository.countByUsuarioAndFechaSorteoBetween(usuario, LocalDate.now().withDayOfMonth(1), LocalDate.now().plusMonths(1).minusDays(1));

        System.out.println("Debug: Usuario ID " + usuario.getId() + ", limiteRifas = " + limiteRifas + ", rifasCreadas = " + rifasCreadas);

        if (rifasCreadas >= limiteRifas) {
            throw new IllegalArgumentException("Has alcanzado el límite de rifas permitidas para este código VIP.");
        }

        // Asignar datos a la rifa
        rifa.setUsuario(usuario);
        rifa.setActive(true);
        if (usuario.isEsVip()) {
            rifa.setCodigoVipUsado(usuario.getCodigoVip());
        }

        Rifa savedRifa = rifaRepository.saveAndFlush(rifa);

        // Decrementar rifas restantes si es VIP
        if (usuario.isEsVip()) {
            this.decrementarCantidadRifas(usuario.getId());
            System.out.println("Rifas restantes para usuario " + usuario.getId() + ": " + usuario.getCantidadRifas());
        }

        return savedRifa;
    }


    public void decrementarCantidadRifas(int userId) {
        Usuario usuario = usuarioRepository.findById(userId).orElse(null);
        if (usuario != null && usuario.isEsVip() && usuario.getCantidadRifas() > 0) {
            usuario.setCantidadRifas(usuario.getCantidadRifas() - 1);
            usuarioRepository.save(usuario);
            System.out.println("Rifas restantes para usuario " + userId + ": " + usuario.getCantidadRifas());
        }
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


    public List<Rifa> obtenerRifasPorUsuarioId(Long usuarioId) {
        List<Rifa> rifas = rifaRepository.findByUsuarioId(usuarioId);
        if (rifas.isEmpty()) {
            return new ArrayList<>(); // Devuelve vacío en lugar de excepción
        }
        return rifas;
    }


    /*public void eliminarRifa(Long id) {
        Rifa rifa = rifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + id));

        // AGREGAR: Eliminar imágenes del volumen
        if (rifa.getProducto() != null && rifa.getProducto().getImagenes() != null) {
            for (String imageUrl : rifa.getProducto().getImagenes()) {
                try {
                    String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                    Path filePath = Paths.get("/app/uploads/" + fileName);
                    if (Files.deleteIfExists(filePath)) {
                        System.out.println("Imagen eliminada del volumen: " + fileName);
                    }
                } catch (IOException e) {
                    System.err.println("Error al eliminar imagen: " + e.getMessage());
                }
            }
        }

        List<Participante> participantes = participanteRepository.findByRaffleId(id);
        if (!participantes.isEmpty()) {
            participanteRepository.deleteAll(participantes);
        }

        rifaRepository.delete(rifa);
    }*/


    public void eliminarRifa(Long id) {
        Rifa rifa = rifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + id));

        // Eliminar imágenes del volumen
        if (rifa.getProducto() != null && rifa.getProducto().getImagenes() != null) {
            for (String imageUrl : rifa.getProducto().getImagenes()) {
                try {
                    String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                    Path filePath = Paths.get("/app/uploads/" + fileName);
                    if (Files.deleteIfExists(filePath)) {
                        System.out.println("Imagen eliminada del volumen: " + fileName);
                    }
                } catch (IOException e) {
                    System.err.println("Error al eliminar imagen: " + e.getMessage());
                }
            }
        }

        // Eliminar participantes
        List<Participante> participantes = participanteRepository.findByRaffleId(id);
        if (!participantes.isEmpty()) {
            participanteRepository.deleteAll(participantes);
        }

        // 🔥 Si VIP y no ejecutada, incrementa rifas restantes
        if (rifa.getUsuario().isEsVip() && !rifa.isExecuted()) {
            int userId = rifa.getUsuario().getId();
            Usuario usuario = usuarioRepository.findById(userId).orElse(null);
            if (usuario != null) {
                usuario.setCantidadRifas(usuario.getCantidadRifas() + 1); // Incrementa 1
                usuarioRepository.save(usuario);
                System.out.println("Rifas incrementadas para usuario " + userId + ": " + usuario.getCantidadRifas() + " (rifa no ejecutada eliminada)");
            }
        }

        // Eliminar rifa
        rifaRepository.delete(rifa);
        System.out.println("Rifa ID " + id + " eliminada completamente.");
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


    public void actualizarPagoRifa(String rifaId, String paymentId) {
        // Implementa lógica: asigna número ganador, set executed = true, notifica usuario
        System.out.println("Pago confirmado para rifa " + rifaId + " con payment ID " + paymentId);
        // Ejemplo: Rifa rifa = rifaRepository.findById(Long.parseLong(rifaId)).orElse(null);
        // rifa.setWinningNumber(randomNumber());
        // rifa.setExecuted(true);
        // rifaRepository.save(rifa);
    }


    public Map<String, Object> crearPreferenciaPago(Long rifaId, BigDecimal amount, Integer usuarioId) {
        Rifa rifa = rifaRepository.findById(rifaId)
                .orElseThrow(() -> new ResourceNotFoundException("Rifa no encontrada con ID: " + rifaId));

        // 🔍 Verificación de datos antes de construir la preferencia
        System.out.println("🔍 Datos recibidos:");
        System.out.println("Rifa ID: " + rifaId);
        System.out.println("Monto recibido: " + amount);
        System.out.println("Usuario ID: " + usuarioId);
        System.out.println("Nombre rifa desde BD: " + rifa.getNombre());
        System.out.println("Precio rifa desde BD: " + rifa.getPrecio());

        try {
            // 🔐 Configurar token de acceso
            MercadoPagoConfig.setAccessToken("APP_USR-2830553727018436-111212-36bcd222790027ee0e3220aa5a01701f-2392507839");


            // 🧾 Crear ítem de la preferencia
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title("Rifa: " + rifa.getNombre())
                    .quantity(1)
                    .unitPrice(amount)
                    .currencyId("ARS")
                    .build();

            // 🔁 URLs de redirección
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://supersorteo-5f1f3.web.app/success")
                    .failure("https://supersorteo-5f1f3.web.app/failure")
                    .pending("https://supersorteo-5f1f3.web.app/pending")
                    .build();

            // 🧠 Construir preferencia completa
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(itemRequest))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .metadata(Map.of(
                            "rifaId", rifaId,
                            "usuarioId", usuarioId
                    ))
                    .build();

            // 🚀 Crear preferencia en Mercado Pago
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            // ✅ Retornar datos al frontend
            return Map.of(
                    "id", preference.getId(),
                    "initPoint", preference.getInitPoint()
            );

        } catch (MPApiException e) {
            System.err.println("❌ MPApiException:");
            System.err.println("Status Code: " + e.getApiResponse().getStatusCode());
            System.err.println("Content: " + e.getApiResponse().getContent());
            System.err.println("Cause: " + e.getCause());
            throw new RuntimeException("Error al crear preferencia MP: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            throw new RuntimeException(e);
        }
    }
}