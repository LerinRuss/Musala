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
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static test.musala.DronesResponses.DroneResponse;

@Controller("/drones")
@RequiredArgsConstructor
public class DronesController {
    public static final String MULTI_PART_NAME_TO_SEND_MEDICINE_BODIES = "medicines";
    public static final String MULTI_PART_NAME_TO_SEND_MEDICINE_IMAGES = "images";
    private final Map<String, Models.Drone> inMemoryDatastore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;


    @Post("/one")
    public DroneResponse register(@Size(min = 1, max = 100) String serialNumber, Models.Drone.Model model,
                         @Min(1) @Max(500) long weightLimitGrams,
                         @Min(0) @Max(100) int batteryCapacityPercentage) {
        var registeredDrone = new Models.Drone(serialNumber, model, weightLimitGrams, batteryCapacityPercentage);
        inMemoryDatastore.put(serialNumber, registeredDrone);

        return new DroneResponse(registeredDrone);
    }

    @Post(value = "/{serialNumber}/medicines", consumes = MediaType.MULTIPART_FORM_DATA)
    public Publisher<DroneResponse> registerMedicine(@PathVariable("serialNumber") String droneSerialNumber,
                                                @Body MultipartBody body) {
        return Mono.create(emitter -> body.subscribe(new Subscriber<CompletedPart>() {
            Map<String, DronesRequests.MedicineRequest> medicinesRequests = new HashMap<>();
            List<Map.Entry<String, CompletedFileUpload>> caseImagesRequests = new ArrayList<>();

            @Override
            public void onSubscribe(Subscription s) {
                s.request(10); // TODO Can I control it?
            }

            @Override
            public void onNext(CompletedPart part) {
                if ((!part.getName().equals(MULTI_PART_NAME_TO_SEND_MEDICINE_BODIES)
                        && !part.getName().equals(MULTI_PART_NAME_TO_SEND_MEDICINE_IMAGES))
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
                emitter.error(t);
            }

            @Override
            public void onComplete() {
                List<Models.Medicine> medicinesWithSavedImage = caseImagesRequests.stream()
                        .map(caseImagesRequest -> {
                            File savedFile;
                            try {
                                savedFile = File.createTempFile(caseImagesRequest.getKey(), "jpg");
                                Files.write(Paths.get(savedFile.getAbsolutePath()), caseImagesRequest.getValue().getBytes());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            DronesRequests.MedicineRequest medicineRequest = medicinesRequests.get(caseImagesRequest.getKey());

                            return new Models.Medicine(
                                    medicineRequest.name(), medicineRequest.weightGrams(),
                                    medicineRequest.code(), savedFile.getPath());
                        }).toList();

                Models.Drone saved = inMemoryDatastore.get(droneSerialNumber);
                saved.getMedicines().addAll(medicinesWithSavedImage);

                emitter.success(new DroneResponse(saved));
            }
        }));
    }

    @Get("/{droneSerialNumber}/")
    public DroneResponse getDrone(String droneSerialNumber) {
        return new DroneResponse(inMemoryDatastore.get(droneSerialNumber));
    }
}

