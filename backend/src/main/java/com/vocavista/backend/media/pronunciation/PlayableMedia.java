package com.vocavista.backend.media.pronunciation;

import java.net.URI;
import java.time.OffsetDateTime;

record PlayableMedia(URI url, OffsetDateTime expiresAt) {
}
