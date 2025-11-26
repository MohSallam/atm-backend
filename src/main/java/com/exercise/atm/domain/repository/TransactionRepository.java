package com.exercise.atm.domain.repository;

import com.exercise.atm.domain.entity.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.accountId = :accountId
              and t.type = com.exercise.atm.domain.entity.TransactionType.WITHDRAWAL
              and t.occurredAt between :startOfDay and :endOfDay
            """)
    BigDecimal sumWithdrawnToday(UUID accountId, Instant startOfDay, Instant endOfDay);
}
