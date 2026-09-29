package com.senai.datum.service;

import javax.management.RuntimeErrorException;

import org.springframework.stereotype.Service; // Biblioteca que permite colocar a anotação service

import com.senai.datum.models.consultor;
import com.senai.datum.repository.consultorRepository;

import jakarta.transaction.Transactional;

// Anotação de service é onde vai ter as regras de negocio

@Service 
public class consultorService {

    // Cria a variavel Consultor repository

    private final consultorRepository repository; // cria a variavel repository que permite manipular o banco de dados


    // Cria o construtor

    public consultorService(
        consultorRepository repository){
            this.repository = repository;
        }


    // ======
    // CREATE
    // ======

    @Transactional 
    public consultor criar(consultor consultor){
        if(consultor.getNome() == null || consultor.getNome().isBlank()){
            throw new RuntimeException(
                "Nome é obrigatório !"
            );
        }


        if(consultor.getEmail() == null || consultor.getEmail().isBlank()){

            throw new RuntimeException(
                "Email é obrigatório !"
            );

        }


        if(consultor.getSenha() == null || consultor.getSenha().isBlank()){
            throw new RuntimeException(
                "Senha é obrigatória !"
            );
        }



        // Verifica se já exise consultor com o mesmo email


        if(repository.findByEmail(consultor.getEmail()).isPresent()){
            throw new RuntimeException("Email já cadastrado");
        }
        return repository.save(consultor);

    }


// Login 
    
// Cria a função

public consultor login(String email, String senha){

    consultor consultor = repository.findByEmail(email).orElseThrow(()->new RuntimeException("Consultor não encontrado"));


    // Validação
    if(!consultor.getSenha().equals(senha)){
        throw new RuntimeException("Senha inválida");
    }

    return consultor;

}


// READ - todos

public List<consultor> listar(){
    return repository.findAll();
}

// READ por ID

public consultor buscarPorId(Long id){
    return repository.f
}

// UPDATE

@Transactional 
public consultor atualizar(
    Long id, consultor dados
){

    consultor consultor = buscarPorId(id);


    consultor.setNome(dados.getNome()); // pega o nome do consultor


    consultor.setEmail(dados.getEmail());


    return  repository.save(consultor);
}


// Delete

@Transactional 
public void excluir(Long id){
    consultor consultor = buscarPorId(id);

    repository.deleteById(
        consultor.getIdLong() );
}

}
