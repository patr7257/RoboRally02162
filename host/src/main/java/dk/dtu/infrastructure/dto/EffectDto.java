package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")

@JsonSubTypes({ @JsonSubTypes.Type(value = CheckpointDto.class, name = "CHECKPOINT"),
                @JsonSubTypes.Type(value = RebootTokenDto.class, name = "REBOOT_TOKEN"),
                @JsonSubTypes.Type(value = AntennaDto.class, name = "ANTENNA")
})
// ConveyorDto, LasterDto ect
// Keeping here for future use

public sealed interface EffectDto permits AntennaDto, CheckpointDto, RebootTokenDto, StartingTileDto, WallDto {
        String kind();
}
