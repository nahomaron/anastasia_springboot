package com.anastasia.Anastasia_BackEnd.model.event;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.group.GroupEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {

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

    private Set<GroupEntity> invitedGroups;

    private Set<UserEntity> invitedUsers;

    @NotNull(message = "Who can see it, is required")
    private EventVisibilityType visibility;

    private Repetition repetition;

    private Set<EventManagerEntity> eventManagers;

}
