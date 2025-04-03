package com.alanquintana.geminiCaller.repositories;

import com.alanquintana.geminiCaller.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
* Message repository that inherits from our JPARepository, we define functions to sort by time and to find chats by id.
*/

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatId(Long chatId);

    Iterable<Message> findByChatIdOrderByTimestampDesc(Long chatId);
    Iterable<Message> findByChatIdOrderByTimestampAsc(Long chatId);
}
