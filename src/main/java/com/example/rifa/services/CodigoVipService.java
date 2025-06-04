package com.example.rifa.services;

import com.example.rifa.entity.CodigoVip;
import com.example.rifa.exception.ResourceNotFoundException;
import com.example.rifa.repository.CodigoVipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CodigoVipService {
    @Autowired
    private CodigoVipRepository codigoVipRepository;


    public String generarCodigoVip(int cantidadRifas) {
        String codigoPrefix = "VIP-";
        int longitudCodigo = 4; // Por defecto para 10 rifas

        if (cantidadRifas == 15) {
            longitudCodigo = 5;
        } else if (cantidadRifas == 30) {
            longitudCodigo = 6;
        }

        // Generar un código aleatorio de la longitud correspondiente
        String codigo = codigoPrefix + UUID.randomUUID().toString().replace("-", "").substring(0, longitudCodigo).toUpperCase();

        // Guardar el código VIP en la base de datos
        CodigoVip codigoVip = new CodigoVip();
        codigoVip.setCodigo(codigo);
        codigoVip.setCantidadRifas(cantidadRifas);
        codigoVip.setUtilizado(false);
        codigoVipRepository.save(codigoVip);

        return codigo;
    }


    public List<CodigoVip> obtenerTodosLosCodigosVip() {
        return codigoVipRepository.findAll();
    }

    public CodigoVip obtenerCodigoVipPorId(Long id) {
        return codigoVipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código VIP no encontrado con ID: " + id));
    }


    public CodigoVip actualizarCodigoVip(Long id, CodigoVip codigoVipActualizado) {
        CodigoVip codigoVipExistente = codigoVipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código VIP no encontrado con ID: " + id));

        codigoVipExistente.setCodigo(codigoVipActualizado.getCodigo());
        codigoVipExistente.setCantidadRifas(codigoVipActualizado.getCantidadRifas());
        codigoVipExistente.setUtilizado(codigoVipActualizado.isUtilizado());

        return codigoVipRepository.save(codigoVipExistente);
    }

    public void eliminarCodigoVip(Long id) {
        CodigoVip codigoVip = codigoVipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Código VIP no encontrado con ID: " + id));
        codigoVipRepository.delete(codigoVip);
    }


}
