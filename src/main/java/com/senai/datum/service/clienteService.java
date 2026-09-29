package com.senai.datum.service;

import org.springframework.stereotype.Service;

import com.senai.datum.models.cliente;
import com.senai.datum.models.consultor;
import com.senai.datum.repository.clienteRepository;
import com.senai.datum.repository.consultorRepository;

import jakarta.transaction.Transactional;

@Service 
public class clienteService {
    
    // Cria variaveis clienterepository e consultorrepository

    private  final clienteRepository clienteRepository;
    private  final consultorRepository consultorRepository;

    // Cria o construtor
    public clienteService(
        clienteRepository clienteRepository,
        consultorRepository consultorRepository
    ){
        this.clienteRepository = clienteRepository;
        this.consultorRepository = consultorRepository;
    }



    // Create 

    @Transactional 
    public cliente criar(Long idConsultor, cliente cliente){

        // Primeiro verifica se o consultor existe

        consultor consultor = consultorRepository.findById(idConsultor).orElseThrow(
            ()->new RuntimeException("Consultor não encontrado")
        );


        // Validação simples

        if(cliente.getNomeEmpresa()==null || cliente.getNomeEmpresa().isBlank()){
            throw new RuntimeException("Nome da empresa é obrigatório");
        }



        if(cliente.getSegmento()==null || cliente.getSegmento().isBlank()){
            throw new RuntimeException(
                "Segmento é obrigatório"
            );
        }


        if(cliente.getFaturamentoAnual()== null){
            throw new RuntimeException(
                "Faturamento anual é obrigatório"
            );
        }


        // Associa o consultor encontrado ao cliente

        cliente.setConsultor(consultor);

        // Agora salva

        return clienteRepository.save(cliente);
    }


    // Read por id

    public 
    cliente buscarPorId(Long id){
        return clienteRepository.findById(id).orElseThrow(
            ()-> new RuntimeException("Cliente não encontrado")
        );
    }

        // Update

    @Transactional 
    public  cliente atualizar(Long id, cliente dados){
        cliente cliente = buscarporId(id);

        cliente.setNomeEmpresa(dados.getNomeEmpresa());

        cliente.setSegmento(dados.getSegmento());


        cliente.setFaturamentoAnual(dados.getFaturamentoAnual());

        cliente.setNivel(dados.getNivel());


        cliente.setStatus(dados.getStatus());

        return  clienteRepository.save(cliente);
    }

    // Delete 

    @Transactional 
    public void excluir(Long id){
        cliente cliente = buscarporId(id);

        clienteRepository.deleteById(cliente.getIdCliente());
    }
}
