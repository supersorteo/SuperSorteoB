package com.example.rifa.repository;

import com.example.rifa.entity.CodigoVip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoVipRepository extends JpaRepository<CodigoVip, Long> {
    Optional<CodigoVip> findByCodigo(String codigo);
}
