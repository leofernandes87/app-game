package com.leo.appgame.repositories;

import com.leo.appgame.enums.StatusJogo;
import com.leo.appgame.models.Jogo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
public class JogoRepository {

    private static final Logger LOG = Logger.getLogger(JogoRepository.class.getName());

    @PersistenceContext(unitName = "appPU")
    private jakarta.persistence.EntityManager em;

    public List<Jogo> findAll(Optional<StatusJogo> statusOpt,
                              Optional<LocalDateTime> deOpt,
                              Optional<LocalDateTime> ateOpt) {
        StringBuilder jpql = new StringBuilder("SELECT j FROM Jogo j WHERE 1=1");
        HashMap<String, Object> params = new HashMap<>();

        statusOpt.ifPresent(s -> {
            jpql.append(" AND j.statusJogo = :status");
            params.put("status", s);
        });
        deOpt.ifPresent(d -> {
            jpql.append(" AND j.dataHoraPartida >= :de");
            params.put("de", d);
        });
        ateOpt.ifPresent(a -> {
            jpql.append(" AND j.dataHoraPartida <= :ate");
            params.put("ate", a);
        });

        jpql.append(" ORDER BY j.dataHoraPartida DESC");

        TypedQuery<Jogo> q = em.createQuery(jpql.toString(), Jogo.class);
        params.forEach(q::setParameter);
        return q.getResultList();
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
