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
public class InstitutionOutput {
    private UUID id;
    private String name;
    private String emailDomain;
    private String type;
    private Boolean verified;
    private Date createdAt;
}
