package com.example.rifa.services;

import com.example.rifa.entity.Participante;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }



    public void sendRaffleExecutionStart(Long rifaId) {
        Map<String, Object> message = new HashMap<>();
        message.put("rifaId", rifaId);
        message.put("estado", "ejecutando");  // 🔥 Variable de estado que indica que la rifa está en ejecución

        System.out.println("📡 Notificación WebSocket: Sorteo ejecutándose...");
        messagingTemplate.convertAndSend("/topic/raffle-execution", message);
        System.out.println("📡 Mensaje enviado a WebSocket: Rifa ejecutada con ID " + rifaId);
    }



    // 🔥 Enviar actualizaciones del conteo regresivo a todos los clientes
    public void sendCountdownUpdate(Long rifaId, int countdownValue) {
        messagingTemplate.convertAndSend("/topic/countdown", Map.of("rifaId", rifaId, "countdownValue", countdownValue));
    }

    // 🔥 Enviar el resultado del sorteo cuando finalice
    public void sendWinnerUpdate(Long rifaId, Participante ganador, int winningNumber) {
        Map<String, Object> data = new HashMap<>();
        data.put("rifaId", rifaId);
        data.put("winningNumber", winningNumber);
        data.put("ganador", ganador);

        messagingTemplate.convertAndSend("/topic/winner", data);
    }

    // 🔥 Notificar que la rifa ha sido ejecutada y su estado actualizado
    public void sendRaffleUpdate(Long usuarioId) {
        messagingTemplate.convertAndSend("/topic/raffle-updates/" + usuarioId, Map.of("usuarioId", usuarioId));
    }

    // 🔥 Enviar evento de ejecución de rifa
    public void sendRaffleExecuted(Long rifaId) {
        messagingTemplate.convertAndSend("/topic/raffle-executed", Map.of("rifaId", rifaId, "message", "Sorteo ejecutado"));
    }
}
