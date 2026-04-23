package com.smartfarm.ai.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<Conversation> findByTenantIdAndUserIdOrderByCreatedAtDesc(Long tenantId, Long userId);
}
