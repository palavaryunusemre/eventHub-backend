package com.eventhub.types.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserResponse {
    private String email;
    private UUID id;
    private String role;
    private String firstName;
    private String lastName;
    private UUID institutionId;
}
