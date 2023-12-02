package test.musala;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DronesRequests {
    @Serdeable
    public static record MedicineRequest(String name, long weightGrams, String code) {
    }
}
