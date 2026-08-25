package com.via.shinvia.oauth2.domain;

import java.io.Serial;
import java.io.Serializable;

public record PendingSocialUser (SocialProvider provider, String providerUserId, String providerEmail)
    implements Serializable {
}
