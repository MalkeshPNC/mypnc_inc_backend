package com.pnc.masters.devtrack;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DevTrackCommentRepository extends JpaRepository<DevTrackComment, Long> {

    List<DevTrackComment> findByTrackIdInOrderByCommentIdAsc(Collection<Long> trackIds);

    List<DevTrackComment> findByTrackIdOrderByCommentIdAsc(Long trackId);
}
