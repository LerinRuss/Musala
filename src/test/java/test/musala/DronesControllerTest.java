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
    public static final Medicine DEFAULT_FIRST_MEDICINE = new Medicine("first_medicine", 10L, "XYZ 1D456345T", "test_medicine_1.jpg");
    public static final Medicine DEFAULT_SECOND_MEDICINE = new Medicine("second_medicine", 15L, "XYZ 2D456345T", "test_medicine_2.jpg");

    @Nested
    @DisplayName("Registering them")
    class DroneRegistration {
        @Test
        public void test_registrationOne() {
            HttpResponse<?> response = registerDrone(DEFAULT_REGISTRATION_REQUEST);

            Assertions.assertEquals(HttpStatus.OK, response.getStatus());
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

            HttpResponse<?> response = registerMedicine(
                    registrationRequest.serialNumber(),
                    List.of(DEFAULT_FIRST_MEDICINE, DEFAULT_SECOND_MEDICINE));

            Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        }

        @Test
        public void test_loading_DroneNotExists() {

        }

        @Test
        public void test_loading_NotSendingAllImagesOrMedicines() {

        }

        @Test
        public void test_loading_SendingMedicinesAndImagesWithDifferentCodes() {

        }

        @Test
        public void test_loading_AttemptLoadingLoadedItemToAnotherDrone() {

        }

        @Test
        public void test_loading_AttemptToSendNotFilePart() {

        }

        @Test
        public void test_loading_AttemptToSendWrongPartNames() {

        }

        @Test
        public void test_loading_AttemptToSendWrongJsonFormat() {

        }

        @Test
        public void test_loading_AttemptToSendBigImage() {

        }

        @Test
        public void test_loading_AttemptToSendManyBigImages() {

        }

        @Test
        public void test_loading_AttemptToLoadDroneWithWrongState() {

        }

        @Test
        public void test_loading_AttemptToLoadMedicineSimultaneously() {

        }

        @Test
        public void test_loading_AttemptToLoadImageWithWrongFormat() {

        }
    }

    @Nested
    @DisplayName("Getting loaded medicine for a drone")
    class MedicineGetting {
        @Test
        void test_getting() throws URISyntaxException {
            DroneRequest registeredDrone = DEFAULT_REGISTRATION_REQUEST;
            Medicine firstRegisteredMedicine = DEFAULT_FIRST_MEDICINE;
            Medicine secondRegisteredMedicine = DEFAULT_SECOND_MEDICINE;

            registerDrone(registeredDrone);
            registerMedicine(registeredDrone.serialNumber(), List.of(firstRegisteredMedicine, secondRegisteredMedicine));


        }
    }

    private HttpResponse<?> registerDrone(DroneRequest registrationRequest) {
        HttpRequest<DroneRequest> request = HttpRequest.POST("/one", registrationRequest);

        return client.toBlocking().exchange(request);
    }

    private HttpResponse<?> registerMedicine(String droneSerialNumber, List<Medicine> medicines) throws URISyntaxException {
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
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE));
    }

    private record DroneRequest (String serialNumber, String model, long weightLimitGrams, int batteryCapacityPercentage) { }
}
