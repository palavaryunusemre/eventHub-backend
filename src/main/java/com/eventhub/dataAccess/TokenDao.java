package com.eventhub.dataAccess;

import com.eventhub.entities.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenDao extends JpaRepository<Token,Long> {
    Optional<Token> findByToken(String tokenFromContext);
}
