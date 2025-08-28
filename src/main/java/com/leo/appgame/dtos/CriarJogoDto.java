package com.leo.appgame.dtos;

import java.time.LocalDateTime;

/**
 * DTO para criar um jogo.
 */
public class CriarJogoDto {

    private String timeA;
    private String timeB;
    private LocalDateTime dataHoraPartida;

    public CriarJogoDto() {
        // empty
    }

    public String getTimeA() {
        return timeA;
    }

    public void setTimeA(String timeA) {
        this.timeA = timeA;
    }

    public String getTimeB() {
        return timeB;
    }

    public void setTimeB(String timeB) {
        this.timeB = timeB;
    }

    public LocalDateTime getDataHoraPartida() {
        return dataHoraPartida;
    }

    public void setDataHoraPartida(LocalDateTime dataHoraPartida) {
        this.dataHoraPartida = dataHoraPartida;
    }
}
