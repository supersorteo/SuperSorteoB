package com.example.rifa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "codigos_vip")
public class CodigoVip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private boolean utilizado;
    private int cantidadRifas;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public boolean isUtilizado() {
        return utilizado;
    }

    public void setUtilizado(boolean utilizado) {
        this.utilizado = utilizado;
    }

    public int getCantidadRifas() {
        return cantidadRifas;
    }

    public void setCantidadRifas(int cantidadRifas) {
        this.cantidadRifas = cantidadRifas;
    }
}
