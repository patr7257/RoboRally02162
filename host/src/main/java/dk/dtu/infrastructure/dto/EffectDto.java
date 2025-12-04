package dk.dtu.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Benjamin Benyo Endahl Hansen
 * @author William Pii Jæger
 * @author Karl Johannes Agerbo
 * @author Bjarke Søderhamn Petersen
 * @author Weihao Mo
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(
        {@JsonSubTypes.Type(value = CheckpointDto.class, name = "CHECKPOINT"),
                @JsonSubTypes.Type(value = BlueConveyorDto.class,  name = "BLUE_CONVEYOR"),
                @JsonSubTypes.Type(value = GreenConveyorDto.class, name = "GREEN_CONVEYOR"),
                @JsonSubTypes.Type(value = RebootTokenDto.class, name = "REBOOT_TOKEN"),
                @JsonSubTypes.Type(value = AntennaDto.class, name = "ANTENNA"),
                @JsonSubTypes.Type(value = WallDto.class, name = "walldto"),
                @JsonSubTypes.Type(value = StartingTileDto.class, name = "startingtile"),
                @JsonSubTypes.Type(value = GearDto.class, name = "geardto"),
                @JsonSubTypes.Type(value = BoardLaserDto.class, name = "board_laser"),
                @JsonSubTypes.Type(value = PitsDto.class, name = "PITS")
        })
// ConveyorDto, LasterDto ect
// Keeping here for future use
public sealed interface EffectDto permits CheckpointDto, GearDto, WallDto, RebootTokenDto, StartingTileDto, AntennaDto, BlueConveyorDto, GreenConveyorDto, BoardLaserDto,PitsDto {
    String kind();
}
