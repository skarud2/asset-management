package com.via.shinvia.lifecycle.survey.dto;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LifecycleTimelineEventResponse {

    private Long eventId;
    private LifecycleEventType eventType;
    private LocalDate targetDate;
}
