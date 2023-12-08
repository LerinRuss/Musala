package test.musala;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static test.musala.DronesResponses.*;

@MicronautTest
@DisplayName("Drone Operations")
public class DronesControllerTest {
    @Inject
    @Client("/drones")
    HttpClient client;

    @Inject
    DronesClient dronesClient;

    public static final DroneRequest DEFAULT_REGISTRATION_REQUEST = new DroneRequest(
            "1TEST3R002016J", "MIDDLEWEIGHT", 250L, 100);
    public static final Models.Medicine DEFAULT_FIRST_MEDICINE = new Models.Medicine("first_medicine", 10L, "XYZ 1D456345T", "test_medicine_1.jpg");
    public static final Models.Medicine DEFAULT_SECOND_MEDICINE = new Models.Medicine("second_medicine", 15L, "XYZ 2D456345T", "test_medicine_2.jpg");

    @Nested
    @DisplayName("Registering them")
    class DroneRegistration {
        @Test
        public void test_registrationOne() {
            DroneRequest request = DEFAULT_REGISTRATION_REQUEST;
            HttpResponse<DroneResponse> response = registerDrone(request);

            Assertions.assertEquals(HttpStatus.OK, response.getStatus());
            var expected = new DroneResponse(
                    request.serialNumber(), Models.Drone.Model.valueOf(request.model()), request.weightLimitGrams(),
                    request.batteryCapacityPercentage(), Models.Drone.State.IDLE, null);
            Assertions.assertEquals(expected, response.body());
        }

        @Test
        public void test_registrationOne_anyFieldMissed() {
            HttpRequest<Map<String, Object>> request = HttpRequest.POST(
                    "/one",
                    Map.of(
                            "serialNumber", "1TEST3R002016J",
                            "weightLimitGrams", 250,
                            "batteryCapacityPercentage", 100));
            HttpResponse<?> response = client.toBlocking().exchange(request);

            Assertions.fail();
        }
    }

    @Nested
    @DisplayName("Loading medicine for a drone")
    class MedicineLoading {
        @Test
        public void test_loading() throws IOException, URISyntaxException {
            DroneRequest registrationRequest = DEFAULT_REGISTRATION_REQUEST;
            registerDrone(registrationRequest);

            var registeredMedicines = List.of(DEFAULT_FIRST_MEDICINE, DEFAULT_SECOND_MEDICINE);
            HttpResponse<DroneResponse> response = registerMedicine(
                    registrationRequest.serialNumber(),
                    registeredMedicines);

            Assertions.assertEquals(HttpStatus.OK, response.getStatus());

            var expected = new DroneResponse(
                    registrationRequest.serialNumber(),
                    Models.Drone.Model.valueOf(registrationRequest.model()),
                    registrationRequest.weightLimitGrams(), registrationRequest.batteryCapacityPercentage(),
                    Models.Drone.State.IDLE,
                    registeredMedicines.stream().map(MedicineResponse::new).toList());
            Assertions.assertEquals(expected, response.body());
        }

        @Test
        public void test_loading_DroneNotExists() {
            Assertions.fail();
        }

        @Test
        public void test_loading_NotSendingAllImagesOrMedicines() {
            Assertions.fail();
        }

        @Test
        public void test_loading_SendingMedicinesAndImagesWithDifferentCodes() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptLoadingLoadedItemToAnotherDrone() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptToSendNotFilePart() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptToSendWrongPartNames() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptToSendWrongJsonFormat() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptToSendBigImage() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptToSendManyBigImages() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptToLoadDroneWithWrongState() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptToLoadMedicineSimultaneously() {
            Assertions.fail();
        }

        @Test
        public void test_loading_AttemptToLoadImageWithWrongFormat() {
            Assertions.fail();
        }
    }

    @Nested
    @DisplayName("Getting loaded medicine for a drone")
    class MedicineGetting {
        @Test
        void test_getting() throws URISyntaxException {
            DroneRequest registeredDrone = DEFAULT_REGISTRATION_REQUEST;
            Models.Medicine firstRegisteredMedicine = DEFAULT_FIRST_MEDICINE;
            Models.Medicine secondRegisteredMedicine = DEFAULT_SECOND_MEDICINE;

            registerDrone(registeredDrone);
            registerMedicine(
                    registeredDrone.serialNumber(),
                    List.of(firstRegisteredMedicine, secondRegisteredMedicine));

            DroneResponse drone = dronesClient.get(registeredDrone.serialNumber());

            var expected = new DroneResponse(
                    registeredDrone.serialNumber(),
                    Models.Drone.Model.valueOf(registeredDrone.model()),
                    registeredDrone.weightLimitGrams(), registeredDrone.batteryCapacityPercentage(),
                    Models.Drone.State.IDLE,
                    List.of(new MedicineResponse(firstRegisteredMedicine), new MedicineResponse(secondRegisteredMedicine)));
            Assertions.assertEquals(expected, drone);
        }
    }

    private HttpResponse<DroneResponse> registerDrone(DroneRequest registrationRequest) {
        HttpRequest<DroneRequest> request = HttpRequest.POST("/one", registrationRequest).accept(MediaType.APPLICATION_JSON);

        return client.toBlocking().exchange(request, DroneResponse.class);
    }

    private HttpResponse<DroneResponse> registerMedicine(String droneSerialNumber, List<Models.Medicine> medicines) throws URISyntaxException {
        ClassLoader classLoader = DronesControllerTest.class.getClassLoader();

        MultipartBody request = medicines.stream().<MultipartBody.Builder>reduce(MultipartBody.builder(),
                (multipartBody, medicine) -> {
                    try {
                        multipartBody.addPart("medicines", medicine.getCode(), MediaType.APPLICATION_JSON_TYPE,
                                    ObjectMapper.getDefault().writeValueAsBytes(
                                            new DronesRequests.MedicineRequest(
                                                    medicine.getName(),
                                                    medicine.getWeightGrams(),
                                                    medicine.getCode())))
                                .addPart("images", medicine.getCode(),
                                        new File(classLoader.getResource(medicine.getImageId()).toURI()));
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }

                    return multipartBody;
                },
                (MultipartBody.Builder b3, MultipartBody.Builder b4) -> {
                    Assertions.assertEquals(b3, b4);

                    return b3;
                }).build();

        return client.toBlocking().exchange(
                HttpRequest.POST(
                                UriBuilder.of("/{drone}/medicines")
                                        .expand(Collections.singletonMap("drone", droneSerialNumber))
                                        .toString(),
                                request)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE),
                DroneResponse.class);
    }

    @Serdeable
    private record DroneRequest (String serialNumber, String model, long weightLimitGrams, int batteryCapacityPercentage) { }
}
