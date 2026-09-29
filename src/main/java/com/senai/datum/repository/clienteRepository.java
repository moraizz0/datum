package com.senai.datum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.senai.datum.models.cliente;

public interface clienteRepository extends JpaRepository<cliente, Long> {
    
}
