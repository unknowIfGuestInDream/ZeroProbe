package com.tlcsdm.zeroprobe.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainFxmlTest {

    @Test
    void connectButtonUsesI18nKeyWithoutInlinePrefix() throws IOException {
        try (InputStream stream = MainFxmlTest.class.getResourceAsStream("/com/tlcsdm/zeroprobe/main.fxml")) {
            assertNotNull(stream, "main.fxml should exist");
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(content.contains("⏻  %connection.connect"),
                "connectButton should not concatenate icon text with i18n key in FXML");
            Pattern pattern = Pattern.compile("fx:id=\"connectButton\"[^>]*text=\"%connection\\.connect\"");
            assertTrue(pattern.matcher(content).find(),
                "connectButton should use i18n key directly");
        }
    }
}
