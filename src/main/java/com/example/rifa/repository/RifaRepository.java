package com.example.rifa.repository;

import com.example.rifa.entity.Rifa;
import com.example.rifa.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface RifaRepository extends JpaRepository<Rifa, Long> {


    @Query("SELECT r FROM Rifa r WHERE r.usuario = :usuario AND r.fechaSorteo BETWEEN :inicioMes AND :finMes")
    List<Rifa> findByUsuarioAndFechaSorteoBetween(
            @Param("usuario") Usuario usuario,
            @Param("inicioMes") LocalDate inicioMes,
            @Param("finMes") LocalDate finMes);

    // Obtener todas las rifas de un usuario por su ID
    List<Rifa> findByUsuarioId(Long usuarioId);
    List<Rifa> findByIsActive(boolean isActive);

    @Query("SELECT COUNT(r) FROM Rifa r WHERE r.usuario = :usuario AND r.fechaSorteo BETWEEN :inicioMes AND :finMes")
    long countByUsuarioAndFechaSorteoBetween(
            @Param("usuario") Usuario usuario,
            @Param("inicioMes") LocalDate inicioMes,
            @Param("finMes") LocalDate finMes);

    //long countByUsuarioAndFechaSorteoBetween(Usuario usuario, LocalDate inicio, LocalDate fin);

    // Contar rifas creadas por un usuario
    long countByUsuario(Usuario usuario);


    List<Rifa> findByExecutedTrue();
}

