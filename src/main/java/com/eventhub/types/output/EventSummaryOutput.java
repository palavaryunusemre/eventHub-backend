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
public class EventSummaryOutput {
    private UUID id;
    private String title;
    private String slug;
    private String coverImageUrl;
    private String categoryName;
    private String format;
    private String location;
    private Date startAt;
    private Date endAt;
    private String status;
    private String visibility;
}
