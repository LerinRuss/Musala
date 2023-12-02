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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

@MicronautTest
public class DronesControllerTest {
    @Inject
    @Client("/drones")
    HttpClient client;

    @Inject
    DronesClient dronesClient;

    @Test
    public void testDronesOne_Post() {
        HttpResponse<?> response = registerDrone("1TEST3R002016J");

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    public void testDronesOne_Post_anyFieldMissed() {
        HttpRequest<Map<String, Object>> request = HttpRequest.POST(
                "/one",
                Map.of(
                        "serialNumber", "1TEST3R002016J",
                        "weightLimitGrams", 250,
                        "batteryCapacityPercentage", 100));
        HttpResponse<?> response = client.toBlocking().exchange(request);

        Assertions.fail();
    }

    @Test
    public void testDronesMedicinesOne_Post() throws IOException, URISyntaxException {
        String droneSerialNumber = "1TEST3R002016J";
        registerDrone(droneSerialNumber);

        var firstCode = "XYZ 1D456345T";
        var secondCode = "XYZ 2D456345T";
        ClassLoader classLoader = DronesControllerTest.class.getClassLoader();
        MultipartBody request = MultipartBody.builder()
                    .addPart("medicines", firstCode, MediaType.APPLICATION_JSON_TYPE,
                            ObjectMapper.getDefault().writeValueAsBytes(
                                    new DronesRequests.MedicineRequest("medicine", 10, firstCode)))
                    .addPart("medicines", secondCode, MediaType.APPLICATION_JSON_TYPE,
                            ObjectMapper.getDefault().writeValueAsBytes(
                                    new DronesRequests.MedicineRequest("medicine", 10, secondCode)))
                    .addPart("images", firstCode, new File(classLoader.getResource("test_medicine_1.jpg").toURI()))
                    .addPart("images", secondCode, new File(classLoader.getResource("test_medicine_2.jpg").toURI()))
                .build();
        HttpResponse<?> response = client.toBlocking().exchange(
            HttpRequest.POST(
                            UriBuilder.of("/{drone}/medicines")
                                .expand(Collections.singletonMap("drone", "droneSerialNumber"))
                                .toString(),
                            request)
                    .contentType(MediaType.MULTIPART_FORM_DATA_TYPE));

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    public void testDronesMedicinesOne_Post_DroneNotExists() {

    }

    @Test
    public void testDronesMedicinesOne_Post_NotSendingAllImagesOrMedicines() {

    }

    @Test
    public void testDronesMedicinesOne_Post_SendingMedicinesAndImagesWithDifferentCodes() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptLoadingLoadedItemToAnotherDrone() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptToSendNotFilePart() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptToSendWrongPartNames() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptToSendWrongJsonFormat() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptToSendBigImage() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptToSendManyBigImages() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptToLoadDroneWithWrongState() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptToLoadMedicineSimultaneously() {

    }

    @Test
    public void testDronesMedicinesOne_Post_AttemptToLoadImageWithWrongFormat() {

    }

    private HttpResponse<?> registerDrone(String serialNumber) {
        HttpRequest<Map<String, Object>> request = HttpRequest.POST(
                "/one",
                Map.of(
                        "serialNumber", serialNumber,
                        "model", "MIDDLEWEIGHT",
                        "weightLimitGrams", 250,
                        "batteryCapacityPercentage", 100));

        return client.toBlocking().exchange(request);
    }
}
