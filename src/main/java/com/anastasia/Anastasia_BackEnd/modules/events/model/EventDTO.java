package com.anastasia.Anastasia_BackEnd.modules.events.model;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {

    private Long eventId;

    private ChurchEntity church;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    private LocalTime endTime;

    private String image;

    private EventType type;

    private Integer capacity;

    private Boolean requiresRegistration;

    private Boolean allowWaitlist;

    private Boolean allowGeoCheckIn;

    private Double latitude;

    private Double longitude;

    private Integer geofenceRadiusMeters;

    private LocalDateTime checkInOpensAt;

    private LocalDateTime checkInClosesAt;

    private Set<GroupEntity> invitedGroups;

    private Set<UserEntity> invitedUsers;

    @NotNull(message = "Who can see it, is required")
    private EventVisibilityType visibility;

    private Repetition repetition;

    private Set<EventManagerEntity> eventManagers;

}
