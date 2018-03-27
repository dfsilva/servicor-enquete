package br.com.anhanguera.enquete.dominio;

import java.io.Serializable;

public class Resposta implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String descricao;
	private Integer qtd;
		
	public Resposta(String descricao, Integer qtd) {
		super();
		this.descricao = descricao;
		this.qtd = qtd;
	}
	
	public String getDescricao() {
		return descricao;
	}
	
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	
	public Integer getQtd() {
		return qtd;
	}
	
	public void setQtd(Integer qtd) {
		this.qtd = qtd;
	}
}
