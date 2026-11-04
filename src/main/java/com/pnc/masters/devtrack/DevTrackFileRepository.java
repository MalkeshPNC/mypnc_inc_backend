package com.pnc.masters.devtrack;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DevTrackFileRepository extends JpaRepository<DevTrackFile, Long> {

    List<DevTrackFile> findByTrackIdInOrderByFileIdAsc(Collection<Long> trackIds);

    List<DevTrackFile> findByTrackIdOrderByFileIdAsc(Long trackId);
}
