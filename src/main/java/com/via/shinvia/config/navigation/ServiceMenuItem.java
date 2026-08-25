package com.via.shinvia.config.navigation;

public record ServiceMenuItem(
        String name,
        String description,
        String path,
        boolean enabled
) {
}
