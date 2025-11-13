package com.example.rifa.repository;

import com.example.rifa.entity.PaymentOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOptionRepository extends JpaRepository<PaymentOption, Long> {
    Optional<PaymentOption> findByBankCode(String bankCode);

    List<PaymentOption> findByUsuarioId(Long usuarioId);
}
