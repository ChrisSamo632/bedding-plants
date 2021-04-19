package uk.co.gmescouts.stmarys.beddingplants.geolocation.service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.ImageResult;
import com.google.maps.StaticMapsApi;
import com.google.maps.StaticMapsRequest.Markers;
import com.google.maps.StaticMapsRequest.Markers.MarkersSize;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.LocationType;
import com.google.maps.model.Size;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Geolocation;
import uk.co.gmescouts.stmarys.beddingplants.exports.model.GeolocatedPoint;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration.GeolocationConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapImageFormat;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerColour;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerSize;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapType;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GeolocationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(GeolocationService.class);

	@Resource
	private GeoApiContext geoApiContext;

	@Resource
	private GeolocationConfiguration geolocationConfiguration;

	public byte[] plotPointsOnMapImage(@NotNull final Set<GeolocatedPoint> points, @NotNull final MapImageFormat mapImageFormat,
			@NotNull final MapType mapType) throws ApiException, InterruptedException, IOException {
		LOGGER.info("Generating Google Map Image of Format [{}] and Type [{}] for [{}] Points", mapImageFormat, mapType, points.size());

		byte[] imgData = null;

		if (!points.isEmpty()) {
			// generate Markers from points (defaultto tiny red markers)
			final Markers markers = new Markers();
			markers.size(MarkersSize.tiny);
			markers.color("red");

			// set Marker Size to be whatever appears most frequently in the Geolocated Points
			final Map<MapMarkerSize, Long> sizeCounts = points.stream().map(GeolocatedPoint::getMapMarkerSize)
					.collect(Collectors.groupingBy(s -> s, Collectors.counting()));
			final Optional<Entry<MapMarkerSize, Long>> mapMarkerSize = sizeCounts.entrySet().stream().max(Entry.comparingByValue());
			mapMarkerSize.ifPresent(mapMarkerSizeLongEntry -> markers.size(mapMarkerSizeLongEntry.getKey().getGoogleStaticMapsMarkerSize()));

			// set Marker Colour to be whatever appears most frequently in the Geolocated Points
			final Map<MapMarkerColour, Long> colourCounts = points.stream().map(GeolocatedPoint::getMapMarkerColour)
					.collect(Collectors.groupingBy(c -> c, Collectors.counting()));
			final Optional<Entry<MapMarkerColour, Long>> mapMarkerColour = colourCounts.entrySet().stream()
					.max(Entry.comparingByValue());
			mapMarkerColour.ifPresent(mapMarkerColourLongEntry -> markers.color(mapMarkerColourLongEntry.getKey().toString()));

			// add point locations
			points.stream().filter(Objects::nonNull).map(GeolocationService::convertGeolocatedPointToLatLng).forEach(markers::addLocation);

			// size of image (from config)
			final Size size = new Size(geolocationConfiguration.getGoogleMapsImgWidth(), geolocationConfiguration.getGoogleMapsImgHeight());

			// create the image (waiting for the return)
			final ImageResult img = StaticMapsApi.newRequest(geoApiContext, size)//
					.scale(geolocationConfiguration.getGoogleMapsImgScale()) //
					.maptype(mapType.getGoogleStaticMapsMapType())//
					.format(mapImageFormat.getGoogleStaticMapsImageFormat())//
					.language(geolocationConfiguration.getGoogleMapsImgLanguage())//
					.markers(markers)//
					.await();

			imgData = img.imageData;
		}

		return imgData;
	}

	public Geolocation geolocateGeolocatableAddress(@NotNull final String geolocatableAddress) {
		LOGGER.info("Gelocating Address [{}]", geolocatableAddress);

		final Geolocation geolocation = new Geolocation();
		try {
			final GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, geolocatableAddress).await();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Geolocation results: [{}]", ArrayUtils.toString(results));
			}

			if (ArrayUtils.isNotEmpty(results)) {
				// look for a ROOFTOP match first
				Optional<GeocodingResult> result = Arrays.stream(results)
						.filter(r -> r.geometry != null && LocationType.ROOFTOP.equals(r.geometry.locationType)).findFirst();

				// then look for non-partial matches
				if (result.isEmpty()) {
					result = Arrays.stream(results).filter(r -> !r.partialMatch).findFirst();
				}

				// fall back to the first entry in the result list
				if (result.isEmpty()) {
					result = Optional.of(results[0]);
				}

				final GeocodingResult selectedResult = result.get();
				LOGGER.debug("Geolocation result: [{}]", selectedResult);

				// set the Geolocation on the Address, assuming something found (otherwise Address not updated)
				if (StringUtils.isNotBlank(selectedResult.formattedAddress)) {
					geolocation.setFormattedAddress(selectedResult.formattedAddress);

					geolocation.setLatitude(selectedResult.geometry.location.lat);
					geolocation.setLongitude(selectedResult.geometry.location.lng);
				}
			}
		} catch (IllegalStateException | ApiException | IOException e) {
			LOGGER.warn(String.format("Unable to geocode address [%s]: %s", geolocatableAddress, e.getMessage()), e);
		} catch (final InterruptedException ie) {
			LOGGER.warn(String.format("Thread interrupted whilst geocoding address [%s]: %s", geolocatableAddress, ie.getMessage()), ie);
			Thread.currentThread().interrupt();
		}

		return geolocation;
	}

	private static LatLng convertGeolocatedPointToLatLng(@NotNull final GeolocatedPoint geolocatedPoint) {
		final LatLng latLng = new LatLng();
		latLng.lat = geolocatedPoint.getLat();
		latLng.lng = geolocatedPoint.getLng();

		return latLng;
	}
}
