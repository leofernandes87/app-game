package com.leo.appgame.services;

import com.leo.appgame.dtos.CriarJogoDto;
import com.leo.appgame.enums.StatusJogo;
import com.leo.appgame.models.Jogo;
import com.leo.appgame.repositories.JogoRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@RequestScoped
public class JogoService {

    @Inject
    private JogoRepository jogoRepository;

    @Transactional
    public Jogo criarNovoJogo(CriarJogoDto criarJogoDto) {
        Jogo jogo = new Jogo();
        jogo.setTimeA(criarJogoDto.getTimeA());
        jogo.setTimeB(criarJogoDto.getTimeB());
        jogo.setDataHoraPartida(criarJogoDto.getDataHoraPartida());
        jogo.setPlacarA(0);
        jogo.setPlacarB(0);
        jogo.setStatusJogo(StatusJogo.EM_ANDAMENTO);
        return jogoRepository.persist(jogo);
    }

    public Jogo buscarJogoPorId(Long id) {
        return jogoRepository.findById(id).orElse(null);
    }

    public List<Jogo> buscarTodosJogos() {
        return jogoRepository.findAll();
    }
}
