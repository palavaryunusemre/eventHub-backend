package com.eventhub.types.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserOutput {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private UUID institutionId;
    private Date createdAt;
}
