package test.musala;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Models {
    @Value
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    @Getter(AccessLevel.PACKAGE)
    static
    class Drone {
        String serialNumber;
        Model model;
        long weightLimitGrams;
        int batteryCapacityPercentage; // TODO How can capacity be percentage? Relative what?
        State state;
        List<Medicine> medicines;

        Drone(String serialNumber, Model model, long weightLimitGrams, int batteryCapacityPercentage) {
            this(serialNumber, model, weightLimitGrams, batteryCapacityPercentage, State.IDLE, new ArrayList<>());
        }

        enum Model {
            LIGHTWEIGHT, MIDDLEWEIGHT, CRUISERWEIGHT, HEAVYWEIGHT
        }

        enum State {
            IDLE, LOADING, LOADED, DELIVERING, DELIVERED, RETURNING
        }
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    static
    class Medicine {
        String name;
        long weightGrams;
        String code;
        String imageId;
    }
}
