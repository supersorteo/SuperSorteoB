package com.example.rifa.services;

import com.example.rifa.entity.Administrador;
import com.example.rifa.repository.AdministradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdministradorService {
    @Autowired
    private AdministradorRepository adminRepo;

    public boolean validarCredenciales(String username, String password) {
        Optional<Administrador> admin = adminRepo.findByUsername(username);
        return admin.isPresent() && admin.get().getPassword().equals(password);
    }

    public boolean cambiarPassword(String username, String nuevaPassword) {
        Optional<Administrador> admin = adminRepo.findByUsername(username);
        if (admin.isPresent()) {
            Administrador administrador = admin.get();
            administrador.setPassword(nuevaPassword);
            adminRepo.save(administrador);
            return true;
        }
        return false;
    }

    public boolean existeAdministrador(String username) {
        return adminRepo.findByUsername(username).isPresent();
    }

    public Administrador crear(Administrador admin) {
        return adminRepo.save(admin);
    }

    public List<Administrador> obtenerTodos() {
        return adminRepo.findAll();
    }

    public Optional<Administrador> obtenerPorId(Long id) {
        return adminRepo.findById(id);
    }

    public Optional<Administrador> obtenerPorUsername(String username) {
        return adminRepo.findByUsername(username);
    }

    public boolean eliminar(Long id) {
        if (adminRepo.existsById(id)) {
            adminRepo.deleteById(id);
            return true;
        }
        return false;
    }

}
