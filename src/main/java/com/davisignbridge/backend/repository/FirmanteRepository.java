package com.davisignbridge.backend.repository;

import com.davisignbridge.backend.domain.Firmante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FirmanteRepository extends JpaRepository<Firmante, UUID> {
    Optional<Firmante> findByToken(UUID token);
}
