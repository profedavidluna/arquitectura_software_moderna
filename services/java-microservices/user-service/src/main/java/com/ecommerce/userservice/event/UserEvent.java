package com.ecommerce.userservice.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent {

    private UUID userId;
    private String eventType;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private LocalDateTime timestamp;

    public enum EventType {
        USER_REGISTERED,
        USER_UPDATED,
        USER_DELETED
    }
}
