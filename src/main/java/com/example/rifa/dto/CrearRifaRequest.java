package com.example.rifa.dto;

import com.example.rifa.entity.Rifa;

public class CrearRifaRequest {

    private Rifa rifa;
    private String codigoVip;

    public Rifa getRifa() {
        return rifa;
    }

    public void setRifa(Rifa rifa) {
        this.rifa = rifa;
    }

    public String getCodigoVip() {
        return codigoVip;
    }

    public void setCodigoVip(String codigoVip) {
        this.codigoVip = codigoVip;
    }
}
