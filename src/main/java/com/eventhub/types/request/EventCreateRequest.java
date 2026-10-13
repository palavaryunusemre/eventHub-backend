package com.eventhub.types.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class EventCreateRequest {
    @NotBlank(message = "Başlık zorunlu")
    private String title;
    private String description;
    private String coverImageUrl;
    private UUID categoryId;
    @NotBlank(message = "Görünürlük zorunlu")
    private String visibility;
    private Set<UUID> allowedInstitutionIds;
    @NotBlank(message = "Format zorunlu")
    private String format;
    private String location;
    private String onlineUrl;
    @NotNull(message = "Başlangıç tarihi zorunlu")
    private Date startAt;
    @NotNull(message = "Bitiş tarihi zorunlu")
    private Date endAt;
    @Positive(message = "Kapasite pozitif olmalı")
    private Integer capacity;
    private Boolean certificateEnabled;
}
