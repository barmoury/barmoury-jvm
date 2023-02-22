package io.github.barmoury.api.repository;

import io.github.barmoury.api.model.BarmourySession;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface BarmourySessionRepository<T extends BarmourySession<?>>
        extends JpaRepository<T, Long>, PagingAndSortingRepository<T, Long>  {

    Page<T> findAllBySessionIdAndStatus(String sessionId, String status, Pageable pageable);

    Optional<T> findByIdAndSessionIdAndStatus(long id, String sessionId, String status);

    Optional<T> findBySessionTokenAndLastAuthTokenAndStatus(String sessionToken, String lastAuthToken, String status);

    @Modifying
    @Query(value = "UPDATE sessions SET status = 'INACTIVE' WHERE id = :id AND status = 'ACTIVE'",
            nativeQuery = true)
    void deleteSelfSession(@Param("id") long id);

    @Modifying
    @Query(value = "UPDATE sessions SET status = 'INACTIVE' WHERE session_id = :sessionId AND status = 'ACTIVE'",
            nativeQuery = true)
    void deleteSelfSessions(@Param("sessionId") String sessionId);

}
