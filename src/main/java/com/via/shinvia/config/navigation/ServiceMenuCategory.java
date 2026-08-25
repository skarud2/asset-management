package com.via.shinvia.config.navigation;

import java.util.List;

public record ServiceMenuCategory(
        String id,
        String name,
        String icon,
        List<ServiceMenuItem> items
) {
}
