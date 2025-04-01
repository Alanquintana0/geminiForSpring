package com.alanquintana.geminiCaller.repositories;


import com.alanquintana.geminiCaller.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, Long> {
}
