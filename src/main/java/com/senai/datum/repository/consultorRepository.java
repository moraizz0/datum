package com.senai.datum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.senai.datum.models.consultor;
import java.util.List;

// interface é uma especie de contrato , o metodo implementado nela sera herdado para outra classe   
public interface consultorRepository extends JpaRepository<consultor, Long> {
   
    // Pelo email irá verificar se o consultor existe ou não
    Optional<consultor>findByEmail(String email);
    
}
