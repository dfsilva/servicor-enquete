package br.com.anhanguera.enquete.dominio;

import java.io.Serializable;
import java.util.List;

public class Enquete implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Long id;
	private String titulo;
	private String descricao;
//	private List<Resposta> respostas;

	
	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

//	public List<Resposta> getRespostas() {
//		return respostas;
//	}
//
//	public void setRespostas(List<Resposta> respostas) {
//		this.respostas = respostas;
//	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
