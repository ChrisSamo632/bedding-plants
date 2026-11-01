package uk.co.gmescouts.stmarys.beddingplants.deliveries.service;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.LatLng;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Geolocation;

import java.io.IOException;
import java.util.List;

@Service
public class RoadDistanceService {
    private static final int MAX_MATRIX_SIDE = 10;

    private final GeoApiContext geoApiContext;

    public RoadDistanceService(final GeoApiContext geoApiContext) {
        this.geoApiContext = geoApiContext;
    }

    public double[][] getRoadDistanceMatrix(final List<Geolocation> locations) {
        final double[][] distances = new double[locations.size()][locations.size()];

        for (int originStart = 0; originStart < locations.size(); originStart += MAX_MATRIX_SIDE) {
            final int originEnd = Math.min(originStart + MAX_MATRIX_SIDE, locations.size());
            final LatLng[] origins = toLatLng(locations.subList(originStart, originEnd));

            for (int destinationStart = 0; destinationStart < locations.size(); destinationStart += MAX_MATRIX_SIDE) {
                final int destinationEnd = Math.min(destinationStart + MAX_MATRIX_SIDE, locations.size());
                final LatLng[] destinations = toLatLng(locations.subList(destinationStart, destinationEnd));
                final DistanceMatrix matrix = requestDistanceMatrix(origins, destinations);

                for (int originIndex = 0; originIndex < origins.length; originIndex++) {
                    final DistanceMatrixRow row = matrix.rows[originIndex];
                    for (int destinationIndex = 0; destinationIndex < destinations.length; destinationIndex++) {
                        final DistanceMatrixElement element = row.elements[destinationIndex];
                        distances[originStart + originIndex][destinationStart + destinationIndex] =
                                distanceInMeters(element, locations.get(originStart + originIndex), locations.get(destinationStart + destinationIndex));
                    }
                }
            }
        }

        return distances;
    }

    private DistanceMatrix requestDistanceMatrix(final LatLng[] origins, final LatLng[] destinations) {
        try {
            return DistanceMatrixApi.newRequest(geoApiContext).origins(origins).destinations(destinations).await();
        } catch (final ApiException | IOException e) {
            throw new IllegalStateException("Unable to calculate road distances for delivery route planning", e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calculating road distances for delivery route planning", e);
        }
    }

    private double distanceInMeters(final DistanceMatrixElement element, final Geolocation origin, final Geolocation destination) {
        if (element.status == DistanceMatrixElementStatus.ZERO_RESULTS) {
            return Double.POSITIVE_INFINITY;
        }
        if (element.status != DistanceMatrixElementStatus.OK || element.distance == null) {
            throw new IllegalStateException(String.format("Unable to calculate road distance between [%s] and [%s]: %s",
                    origin.formattedAddress(), destination.formattedAddress(), element.status));
        }
        return element.distance.inMeters;
    }

    private LatLng[] toLatLng(final List<Geolocation> locations) {
        return locations.stream().map(location -> new LatLng(location.latitude(), location.longitude())).toArray(LatLng[]::new);
    }
}
