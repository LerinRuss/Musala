package test.musala;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.multipart.CompletedPart;
import io.micronaut.http.server.multipart.MultipartBody;
import io.micronaut.serde.ObjectMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller("/drones")
@RequiredArgsConstructor
public class DronesController {
    public static final String MULTI_PART_NAME_TO_SEND_MEDICINE_BODIES = "medicines";
    public static final String MULTI_PART_NAME_TO_SEND_MEDICINE_IMAGES = "images";
    private final Map<String, Drone> inMemoryDatastore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;


    @Post("/one")
    public void register(@Size(min = 1, max = 100) String serialNumber, Drone.Model model,
                         @Min(1) @Max(500) long weightLimitGrams,
                         @Min(0) @Max(100) int batteryCapacityPercentage) {
        inMemoryDatastore.put(serialNumber, new Drone(serialNumber, model, weightLimitGrams, batteryCapacityPercentage));
    }

    @Post(value = "/{serialNumber}/medicines", consumes = MediaType.MULTIPART_FORM_DATA)
    public void registerMedicine(@PathVariable("serialNumber") String droneSerialNumber,
                                 @Body MultipartBody body) {
        body.subscribe(new Subscriber<CompletedPart>() {
            Map<String, DronesRequests.MedicineRequest> medicinesRequests = new HashMap<>();
            List<Map.Entry<String, CompletedFileUpload>> caseImagesRequests = new ArrayList<>();

            @Override
            public void onSubscribe(Subscription s) {
                s.request(10); // TODO Can I control it?
            }

            @Override
            public void onNext(CompletedPart part) {
                if (!part.getName().equals(MULTI_PART_NAME_TO_SEND_MEDICINE_BODIES)
                        || !part.getName().equals(MULTI_PART_NAME_TO_SEND_MEDICINE_IMAGES)
                        || !(part instanceof CompletedFileUpload fileUpload)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Supported part names: %s and %s and only file type",
                                    MULTI_PART_NAME_TO_SEND_MEDICINE_BODIES,
                                    MULTI_PART_NAME_TO_SEND_MEDICINE_IMAGES));
                }

                if (fileUpload.getName().equals(MULTI_PART_NAME_TO_SEND_MEDICINE_BODIES)) {
                    try {
                        medicinesRequests.put(
                            fileUpload.getFilename(),
                            objectMapper.readValue(fileUpload.getBytes(), DronesRequests.MedicineRequest.class));
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Wrong format.", e);
                    }
                } else if (fileUpload.getName().equals(MULTI_PART_NAME_TO_SEND_MEDICINE_IMAGES)) {
                    caseImagesRequests.add(new AbstractMap.SimpleEntry<>(fileUpload.getFilename(), fileUpload));
                }
            }

            @Override
            public void onError(Throwable t) {
                // TODO what to do here?
            }

            @Override
            public void onComplete() {
                List<Medicine> medicinesWithSavedImage = caseImagesRequests.stream()
                                .map(caseImagesRequest -> {
                                    File savedFile;
                                    try {
                                        savedFile = File.createTempFile(caseImagesRequest.getKey(), "jpg");
                                        Files.write(Paths.get(savedFile.getAbsolutePath()), caseImagesRequest.getValue().getBytes());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }

                                    DronesRequests.MedicineRequest medicineRequest = medicinesRequests.get(caseImagesRequest.getKey());

                                    return new Medicine(
                                            caseImagesRequest.getKey(), medicineRequest.weightGrams(),
                                            medicineRequest.code(), savedFile.getPath());
                                }).toList();

                inMemoryDatastore.get(droneSerialNumber).getMedicines().addAll(medicinesWithSavedImage);
            }
        });
    }
}

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class Drone {
    String serialNumber;
    Model model;
    long weightLimitGrams;
    int batteryCapacityPercentage; // TODO How can capacity be percentage? Relative what?
    State state;
    List<Medicine> medicines;

    public Drone(String serialNumber, Drone.Model model, long weightLimitGrams, int batteryCapacityPercentage) {
        this(serialNumber, model, weightLimitGrams, batteryCapacityPercentage, State.IDLE, new ArrayList<>());
    }

    enum Model {
        LIGHTWEIGHT, MIDDLEWEIGHT,CRUISERWEIGHT,HEAVYWEIGHT
    }

    enum State {
        IDLE, LOADING, LOADED, DELIVERING, DELIVERED, RETURNING
    }
}

@Value
class Medicine {
    String name;
    long weightGrams;
    String code;
    String imageId;
}
