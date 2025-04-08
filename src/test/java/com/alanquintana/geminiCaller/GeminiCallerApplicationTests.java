package com.alanquintana.geminiCaller;

import static org.assertj.core.api.Assertions.assertThat;
import com.alanquintana.geminiCaller.controllers.ChatController;
import com.alanquintana.geminiCaller.services.GeminiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class GeminiCallerApplicationTests {

        @MockBean
        private GeminiService geminiService;

        @Autowired
        private ChatController chatController;

        @Test
        void contextLoads(){
            assertThat(chatController).isNotNull();
        }
}
