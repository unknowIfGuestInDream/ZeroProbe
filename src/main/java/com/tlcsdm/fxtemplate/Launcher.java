package com.tlcsdm.fxtemplate;

/**
 * Launcher class for the application.
 * This class serves as the entry point when running from a shaded/fat JAR.
 * It allows the JavaFX runtime to be properly initialized without requiring
 * the main class to extend Application.
 */
public class Launcher {

    public static void main(String[] args) {
        TemplateApplication.main(args);
    }
}
