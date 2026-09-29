package com.senai.datum.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

import com.senai.datum.models.consultor;
import com.senai.datum.service.consultorService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


// Arquivo controller é responsável por realizar as requisições http da API

@RestController // indica que a classe consultor controller irá receber as requisições http
@RequestMapping("/consultores") // cria a rota consultores
public class consultorController {

    // Cria a variavel ConsultorService 
    private final ConsultorService service;

    // Cria o construtor
    public consultorController(
        ConsultorService service){
            this.service = service;
        }

// ======
//CREATE
// =======

@PostMapping
public consultor criar(
    @RequestBody consultor consultor){
        return service.criar(consultor);
    }



// =====
// LOGIN
//

@PostMapping("/login")
public consultor login(@RequestBody consultor consultor){

    return service.login(consultor.getEmail(), consultor.getSenha());

}

//====
// READ

@GetMapping
public List<consultor> listar(){
    return service.listar();
}

// === 
// READ por id
// ====

@GetMapping("/{id}")
public consultor buscar(
    @PathVariable  Long id){
        return service.buscarPorId(id);
    }

// ==== 
// UPDATE
// ====

@PutMapping("/{id}")
public consultor atualizar(
    @PathVariable Long id, @RequestBody consultor consultor){
        return  service.atualizar(id, consultor);
    }

// ==== 
// DELETE
// =====

@DeleteMapping("/{id}")
public void excluir(@PathVariable Long id){
    service.excluir(id);
}
    
}


