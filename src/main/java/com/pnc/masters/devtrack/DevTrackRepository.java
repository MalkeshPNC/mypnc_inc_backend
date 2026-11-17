package com.pnc.masters.devtrack;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevTrackRepository extends JpaRepository<DevTrack, Long> {

    List<DevTrack> findByDeletedFalseOrderByCreatedAtDescTrackIdDesc();
}
