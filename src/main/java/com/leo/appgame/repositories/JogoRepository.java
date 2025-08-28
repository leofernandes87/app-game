package com.leo.appgame.repositories;

import com.leo.appgame.models.Jogo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
public class JogoRepository {

    private static final Logger LOG = Logger.getLogger(JogoRepository.class.getName());

    @PersistenceContext(unitName = "appPU")
    private jakarta.persistence.EntityManager em;

    public List<Jogo> findAll() {
        LOG.info("Buscando todos os Jogos...");
        return em.createQuery("SELECT j FROM Jogo j", Jogo.class).getResultList();
    }

    public Optional<Jogo> findById(Long id) {
        LOG.info("Buscando Jogo por Id...");
        return Optional.ofNullable(em.find(Jogo.class, id));
    }

    @Transactional
    public Jogo persist(Jogo jogo) {
        LOG.info("Persistindo Jogo: " + jogo.getTimeA() + " vs " + jogo.getTimeB());
        em.persist(jogo);
        return jogo;
    }
}
