package uk.co.gmescouts.stmarys.beddingplants.exports.service;

import com.google.maps.errors.ApiException;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.CompressionConstants;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.co.gmescouts.stmarys.beddingplants.data.AddressRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.OrderRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.PlantRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Address;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Geolocation;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderItem;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.exports.ExportHtml;
import uk.co.gmescouts.stmarys.beddingplants.exports.model.GeolocatedPoint;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapImageFormat;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerColour;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerSize;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapType;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.service.GeolocationService;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExportService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExportService.class);

	@Resource
	private GeolocationService geolocationService;

	@Resource
	private OrderRepository orderRepository;

	@Resource
	private PlantRepository plantRepository;

	@Resource
	private AddressRepository addressRepository;

	@Resource
	private RestTemplate restTemplate;

	@Value("${server.ssl.enabled:false}")
	private boolean httpsEnabled;

	private final String hostname;

	@Value("${server.port}")
	private int port;

	@Value("${server.servlet.context-path}")
	private String baseUri;

	private ExportService() throws UnknownHostException {
		hostname = InetAddress.getLocalHost().getHostName();
	}

	private String getExportHostUrl() {
		return String.format("%s://%s:%d", httpsEnabled ? "https" : "http", hostname, port);
	}

	public Set<Order> getSaleCustomerOrders(@NotNull final Integer saleYear, final OrderType orderType,
											final String sortField, final Sort.Direction sortDirection) {
		LOGGER.info("Get Customer Orders for Sale [{}]", saleYear);

		Set<Order> orders;
		if (orderType == null) {
			orders = orderRepository.findByCustomerSaleYear(saleYear, Sort.by(sortDirection, sortField));
		} else {
			orders = orderRepository.findByTypeAndCustomerSaleYear(orderType, saleYear, Sort.by(sortDirection, sortField));
		}

		return orders;
	}

	public Set<Plant> getSalePlants(@NotNull final Integer saleYear) {
		LOGGER.info("Get Plants for Sale [{}]", saleYear);

		return plantRepository.findBySaleYear(saleYear);
	}

	public byte[] exportSaleCustomersToPdf(@NotNull final Integer saleYear, final OrderType orderType,
										   @NotNull final String sortField, @NotNull final Sort.Direction sortDirection)
			throws IOException {

		LOGGER.info("Exporting Customer Orders for Sale [{}] and Order Type [{}]", saleYear, orderType);

		// setup the URLs
		final String exportHostUrl = getExportHostUrl();
		final StringBuilder sb = new StringBuilder(100);
		sb.append("sortField=").append(sortField).append("&sortDirection=").append(sortDirection);
		if (orderType != null) {
			sb.append("&orderType=").append(orderType);
		}

		final String exportHtmlUrl = String.format("%s%s%s?%s", exportHostUrl, baseUri, ExportHtml.EXPORT_CUSTOMER_ORDERS_HTML,
				sb.toString());
		LOGGER.debug("Calling HTML Export URL [{}]", exportHtmlUrl);

		// get the HTML via external call
		final String html = restTemplate.getForObject(exportHtmlUrl, String.class, saleYear);

		// convert HTML to PDF
		byte[] pdf;

		// converter properties (image/css locations)
		final ConverterProperties converterProperties = new ConverterProperties();
		converterProperties.setBaseUri(exportHostUrl);

		// writer properties (compression)
		final WriterProperties writerProperties = new WriterProperties();
		writerProperties.setCompressionLevel(CompressionConstants.BEST_COMPRESSION);

		// converter
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			final PdfWriter pdfWriter = new PdfWriter(baos, writerProperties);
			HtmlConverter.convertToPdf(html, pdfWriter, converterProperties);
			pdf = baos.toByteArray();
		}

		// return converted PDF (if any)
		return pdf;
	}

	public byte[] exportSaleCustomersToCsv(@NotNull final Integer saleYear, final OrderType orderType,
										   final String sortField, final Sort.Direction sortDirection) throws IOException {
		LOGGER.info("Exporting Customer Orders for Sale [{}] and Order Type [{}]", saleYear, orderType);

		final byte[] csv;
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 final CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(baos), CSVFormat.DEFAULT)) {

			// get the orders
			final Set<Order> orders = getSaleCustomerOrders(saleYear, orderType, sortField, sortDirection);

			// get the plants
			final Set<Plant> plants = getSalePlants(saleYear);

			// output each Order to the CSV
			for (final Order order : orders) {
				final Customer customer = order.getCustomer();

				final List<String> items = new ArrayList<>(List.of(
						Integer.toString(order.getNum()),
						customer.getName(),
						StringUtils.capitalize(order.getType().toString()),
						StringUtils.capitalize(order.getDeliveryDay().toString()),
						StringUtils.capitalize(order.getCollectionSlot().toString()),
						Integer.toString(order.getCollectionHour()),
						customer.getAddress().getGeolocatableAddress(),
						customer.getEmailAddress(),
						customer.getTelephone()
				));

				plants.forEach(plant -> {
					final Optional<OrderItem> orderItem = order.getOrderItems().stream().filter(i -> i.getPlant().equals(plant)).findFirst();
					items.add(orderItem.map(item -> Integer.toString(item.getCount())).orElse("0"));
				});

				csvPrinter.printRecord(items);
			}

			csvPrinter.flush();
			csv = baos.toByteArray();
		}

		// return CSV content
		return csv;
	}

	public Set<GeolocatedPoint> getGeolocatedSaleAddressesAsPoints(@NotNull final Integer saleYear, final OrderType orderType) {
		LOGGER.info("Generating Geolocated Points from Addresses for Sale Year [{}] and Order Type [{}]", saleYear, orderType);

		final Set<Address> geolocatedAddresses = getSaleAddresses(saleYear, orderType, true);
		LOGGER.debug("[{}] Geolocated Addresses", CollectionUtils.size(geolocatedAddresses));

		Set<GeolocatedPoint> geolocatedPoints = Collections.emptySet();
		if (geolocatedAddresses != null) {
			// convert Addresses to GeolocatedPoints ready for plotting on the map
			geolocatedPoints = geolocatedAddresses.stream().map(ExportService::convertAddressToGeolocatedPoint).collect(Collectors.toSet());
			LOGGER.debug("[{}] Geolocated Points from Addresses", CollectionUtils.size(geolocatedPoints));
		}

		return geolocatedPoints;
	}

	public byte[] exportGeolocatedSaleAddressesToImage(@NotNull final Integer saleYear, final OrderType orderType,
			@NotNull final MapImageFormat mapImageFormat, @NotNull final MapType mapType) throws ApiException, InterruptedException, IOException {
		LOGGER.info("Generating Map Image for Addresses from Sale Year [{}] and Order Type [{}] in Format [{}] with Map Type [{}]", saleYear,
				orderType, mapImageFormat, mapType);

		// get the (geolocated) Addresses as Points
		final Set<GeolocatedPoint> geolocatedPoints = getGeolocatedSaleAddressesAsPoints(saleYear, orderType);

		// generate the image
		byte[] mapImg = null;
		if (CollectionUtils.isNotEmpty(geolocatedPoints)) {
			// get the image containing the Geolocated Points
			mapImg = geolocationService.plotPointsOnMapImage(geolocatedPoints, mapImageFormat, mapType);
			LOGGER.debug("Generated Image size [{}]", mapImg == null ? 0 : mapImg.length);
		}

		return mapImg;
	}

	public Set<Address> getSaleAddresses(@NotNull final Integer saleYear, final OrderType orderType, final boolean geolocatedOnly) {
		LOGGER.info("Retrieving Addresses for Sale Year [{}] and Order Type [{}], Geolocated Only [{}]", saleYear, orderType, geolocatedOnly);

		// get Addresses for specified Sale Year/OrderType
		Set<Address> addresses;
		if (orderType != null) {
			addresses = addressRepository.findAddressByCustomersSaleYearAndCustomersOrdersType(saleYear, orderType);
		} else {
			addresses = addressRepository.findAddressByCustomersSaleYear(saleYear);
		}
		LOGGER.debug("[{}] Sale Addresses", CollectionUtils.size(addresses));

		// filter to geolocated addresses only if so requested
		if (geolocatedOnly) {
			// geolocate Address(es), if not already
			if (CollectionUtils.isNotEmpty(addresses)) {
				LOGGER.debug("Geolocating Sale Addresses (if not previously geolocated)");
				addresses.forEach(this::geolocateAddress);

				// save these to the database
				addresses = new HashSet<>(addressRepository.saveAll(addresses));
			}

			addresses = addresses.stream().filter(Address::isGeolocated).collect(Collectors.toSet());
			LOGGER.debug("[{}] Geolocated Sale Addresses", CollectionUtils.size(addresses));
		}

		return addresses;
	}

	private static GeolocatedPoint convertAddressToGeolocatedPoint(@NotNull final Address address) {
		LOGGER.info("Converting Address [{}] to Geolocated Point", address);

		final GeolocatedPoint geolocatedPoint = new GeolocatedPoint(address.getGeolocation().getLatitude(), address.getGeolocation().getLongitude(),
				address.getGeolocatableAddress());

		// determine size of the marker based on number of orders
		// TODO: does this need restricting to specific Sale Year (as Customer may be a repeat across multiple Years)? Same for Order Types below?
		final long numOrders = address.getCustomers().stream().mapToLong(customer -> customer.getOrders().size()).sum();
		LOGGER.debug("Number of Orders associated with this Address [{}]", numOrders);
		if (numOrders == 1) {
			geolocatedPoint.setMapMarkerSize(MapMarkerSize.NORMAL);
		} else {
			geolocatedPoint.setMapMarkerSize(MapMarkerSize.MID);
		}

		// determine colour of marker based on order type
		final boolean delivery = address.getCustomers().stream().flatMap(customer -> customer.getOrders().stream()).map(Order::getType)
				.anyMatch(OrderType::isDelivery);
		LOGGER.debug("Address contains a Delivery [{}]", delivery);
		if (delivery) {
			geolocatedPoint.setMapMarkerColour(MapMarkerColour.RED);
		} else {
			geolocatedPoint.setMapMarkerColour(MapMarkerColour.GREEN);
		}

		LOGGER.debug("Generated Geolocated Point [{}]", geolocatedPoint);

		return geolocatedPoint;
	}

	private void geolocateAddress(@NotNull final Address address) {
		if (address.getGeolocation() == null && address.isGeolocatable()) {
			final String geolocatableAddress = address.getGeolocatableAddress();
			LOGGER.debug("Geolocatable Address [{}]", geolocatableAddress);

			final Geolocation geolocation = geolocationService.geolocateGeolocatableAddress(geolocatableAddress);
			if (geolocation != null && StringUtils.isNoneBlank(geolocation.getFormattedAddress())) {
				address.setGeolocation(geolocation);
			}
		}
	}
}
