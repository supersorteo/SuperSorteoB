package com.example.rifa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.persistence.*;

@Entity
@Table(name = "payment_options")
public class PaymentOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código del banco es obligatorio")
    @Size(max = 2, message = "El código del banco debe tener máximo 2 caracteres")
    private String bankCode;

    @NotBlank(message = "El alias es obligatorio")
    @Size(max = 50, message = "El alias debe tener máximo 50 caracteres")
    private String alias;



    //@Pattern(regexp = "^\\d{22}$", message = "El CBU/CVU debe tener exactamente 22 dígitos")
    @Column(name = "cbu", nullable = true)
    private String cbu;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "mp_payment_id", length = 255) // Nuevo: ID pago MP para tracking
    private String mpPaymentId;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank(message = "El código del banco es obligatorio") @Size(max = 2, message = "El código del banco debe tener máximo 2 caracteres") String getBankCode() {
        return bankCode;
    }

    public void setBankCode(@NotBlank(message = "El código del banco es obligatorio") @Size(max = 2, message = "El código del banco debe tener máximo 2 caracteres") String bankCode) {
        this.bankCode = bankCode;
    }

    public @NotBlank(message = "El alias es obligatorio") @Size(max = 50, message = "El alias debe tener máximo 50 caracteres") String getAlias() {
        return alias;
    }

    public void setAlias(@NotBlank(message = "El alias es obligatorio") @Size(max = 50, message = "El alias debe tener máximo 50 caracteres") String alias) {
        this.alias = alias;
    }



    public String getCbu() {
        return cbu;
    }

    public void setCbu(String cbu) {
        this.cbu = cbu;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getMpPaymentId() { return mpPaymentId; }
    public void setMpPaymentId(String mpPaymentId) { this.mpPaymentId = mpPaymentId; }
}
