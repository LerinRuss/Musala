package test.musala;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

import static test.musala.Models.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DronesResponses {
    @Serdeable
    public static record DroneResponse(String serialNumber, Drone.Model model, long weightLimitGrams, int batteryCapacityPercentage,
                                       Drone.State state, List<MedicineResponse> medicines) {
        DroneResponse(Drone drone) {
            this(drone.getSerialNumber(), drone.getModel(), drone.getWeightLimitGrams(),
                    drone.getBatteryCapacityPercentage(), drone.getState(),
                    drone.getMedicines().stream().map(MedicineResponse::new).toList());
        }
    }

    @Serdeable
    public static record MedicineResponse(String name, long weightGrams, String code) {
        MedicineResponse(Medicine medicine) {
            this(medicine.getName(), medicine.getWeightGrams(), medicine.getCode());
        }
    }
}
