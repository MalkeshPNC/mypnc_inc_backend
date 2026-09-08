package com.pnc.masters.quote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuoteLockRepository extends JpaRepository<QuoteLock, Long> {

    Optional<QuoteLock> findByQid(Long qid);

    List<QuoteLock> findAllByQidIn(Collection<Long> qids);

    void deleteByQid(Long qid);
}
