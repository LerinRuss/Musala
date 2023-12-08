package test.musala;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

@Client("/drones")
public interface DronesClient {
    @Get("/{droneSerialNumber}")
    DronesResponses.DroneResponse get(String droneSerialNumber);
}
