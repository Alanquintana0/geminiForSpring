package com.alanquintana.geminiCaller.repositories;

import com.alanquintana.geminiCaller.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatId(Long chatId);

    Iterable<Message> findByChatIdOrderByTimestampDesc(Long chatId);
    Iterable<Message> findByChatIdOrderByTimestampAsc(Long chatId);
}
