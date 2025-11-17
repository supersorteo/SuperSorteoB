package com.example.rifa.repository;

import com.example.rifa.entity.CodigoVip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoVipRepository extends JpaRepository<CodigoVip, Long> {
    Optional<CodigoVip> findByCodigo(String codigo);

    @Query("SELECT c FROM CodigoVip c WHERE c.usuarioAsignado.id = :usuarioId")
    Optional<CodigoVip> findByUsuarioId(@Param("usuarioId") Integer usuarioId);

    @Query("SELECT c FROM CodigoVip c WHERE c.usuarioAsignado.id = :usuarioId")
    List<CodigoVip> findAllByUsuarioId(@Param("usuarioId") Integer usuarioId);

    @Query("SELECT c FROM CodigoVip c JOIN FETCH c.usuarioAsignado u WHERE c.utilizado = true ORDER BY c.id DESC")
    List<CodigoVip> findAllSoldCodesWithUserDetails();
}
