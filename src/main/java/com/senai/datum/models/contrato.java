package com.senai.datum.models;

import java.math.BigDecimal;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity; // biblioteca para persistencia de dados
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity 
@Table(name="contrato") // Cria tabela chamada contrato
public class contrato {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name= "id_contrato")
    private  Long idContrato; // id do contrato

    // Relacionamento para cliente
    @ManyToOne
    @JoinColumn (name= "id_cliente", nullable = false)
    private  cliente cliente; // Atributo cliente do tipo cliente

    @Column(name = "nome_empresa",nullable = false,length = 180)
    private  String nomeEmpresa;

    @Column (name="segmento",nullable = false, length = 100)
    private String segmento;


    @Column(
        name = "faturamento_anual",
        nullable = false,
        precision = 15,
        scale=2
    )

    private BigDecimal faturamentoAnual;

    @Enumerated(EnumType.STRING)
    @Column (name = "nivel",nullable = false)
    private nivelCliente nivel;

    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false)
    private statusCliente status;

    public contrato() {
    }

    public contrato(cliente cliente, String nomeEmpresa, String segmento, BigDecimal faturamentoAnual, nivelCliente nivel, statusCliente status) {
        this.cliente = cliente;
        this.nomeEmpresa = nomeEmpresa;
        this.segmento = segmento;
        this.faturamentoAnual = faturamentoAnual;
        this.nivel = nivel;
        this.status = status;
    }

    // Cria função para pegar o ID
    public Long getIdContrato() {
        return idContrato;
    }

    public cliente getCliente() {
        return cliente;
    }

    public void setCliente(cliente cliente) {
        this.cliente = cliente;
    }

    // Função para pegar o nome da empresa
    public String getNomeEmpresa(){
        return nomeEmpresa;
    }

    // Função para setar o nome da empresa
    public void setNomeEmpresa(String nomeEmpresa){
        this.nomeEmpresa = nomeEmpresa;
    }

    // Função para pegar o segmento
    public String getSegmento(){
        return segmento;
    }

    // Função para pegar o faturamento
    public BigDecimal getFaturamentoAnual(){
        return faturamentoAnual;
    }

    public void setFaturamentoAnual(BigDecimal faturamentoAnual){
        this.faturamentoAnual = faturamentoAnual;
    }

    public nivelCliente getNivel() {
        return nivel;
    }

    public void setNivel(nivelCliente nivel){
        this.nivel = nivel;
    }

    public statusCliente getStatus(){
        return status;
    }

    public void setStatus(statusCliente status){
        this.status = status;
    }
}
