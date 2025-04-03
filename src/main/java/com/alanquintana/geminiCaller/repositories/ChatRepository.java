package com.alanquintana.geminiCaller.repositories;


import com.alanquintana.geminiCaller.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
* Chat repository that inherits from JpaRepository, this repository
* defines methods for sort chats by time in descendant order.
*/

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllByOrderByCreatedAtDesc();
}
