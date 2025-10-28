package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind"
)

@JsonSubTypes(
        {@JsonSubTypes.Type(value = CheckpointDto.class, name = "CHECKPOINT")
        }
)
// ConveyorDto, LasterDto ect
// Keeping here for future use
public sealed interface EffectDto permits CheckpointDto, WallDto {
    String kind();
}
