package com.eventhub.types.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class InstitutionRequest {
    @NotBlank(message = "Kurum adı zorunlu")
    private String name;
    private String emailDomain;
    @NotBlank(message = "Kurum türü zorunlu")
    private String type;
}
