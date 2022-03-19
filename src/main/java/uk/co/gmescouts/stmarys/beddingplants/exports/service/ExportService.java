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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.co.gmescouts.stmarys.beddingplants.data.AddressRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Address;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Geolocation;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderItem;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.exports.ExportHtml;
import uk.co.gmescouts.stmarys.beddingplants.exports.model.GeolocatedPoint;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapImageFormat;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerColour;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerSize;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapType;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.service.GeolocationService;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.plants.service.PlantsService;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ExportService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExportService.class);

	private static final Charset CSV_CHARSET = StandardCharsets.UTF_8;
	private static final byte[] CSV_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
	private static final char POUND_SIGN = '\u00A3';

	@Resource
	private GeolocationService geolocationService;

	@Resource
	private OrdersService ordersService;

	@Resource
	private PlantsService plantsService;

	@Resource
	private SalesService salesService;

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

	public String valueOrEmpty(final String value) {
		return value == null ? "" : value;
	}

	public byte[] exportSaleCustomersToPdf(@NotNull final Integer saleYear, final OrderType orderType, @NotNull final String sorts)
			throws IOException {

		LOGGER.info("Exporting Customer Orders for Sale [{}] and Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);

		// setup the URLs
		final String exportHostUrl = getExportHostUrl();
		final StringBuilder sb = new StringBuilder(100);
		sb.append("sorts=").append(sorts);
		if (orderType != null) {
			sb.append("&orderType=").append(orderType);
		}

		final String exportHtmlUrl = String.format("%s%s%s?%s", exportHostUrl, baseUri, ExportHtml.EXPORT_CUSTOMER_ORDERS_HTML, sb);
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

	private void addPlantAndPaymentDetails(final List<String> items, final Set<Plant> plants, final Order order) {
		plants.forEach(plant -> {
			final Optional<OrderItem> orderItem = order.getOrderItems().stream().filter(i -> i.getPlant().equals(plant)).findFirst();
			items.add(orderItem.map(item -> Integer.toString(item.getCount())).orElse("0"));
		});

		items.add(Integer.toString(order.getCount()));
		items.add(String.format("%.2f", order.getDisplayPrice()));
		items.add(order.getDiscount() == null ? "" : String.format("%.2f", order.getDisplayDiscount()));
		items.add(order.getPaid() == null ? "" : String.format("%.2f", order.getDisplayPaid()));
		items.add(order.getToPay() == 0 ? "0.00" : String.format("%.2f", order.getDisplayToPay()));
	}

	private String getCollectionHour(final Order order) {
		return order.getCollectionHour() == null ? "" : Integer.toString(order.getCollectionHour());
	}

	private String getAddress(final Customer customer) {
		return customer.getAddress() == null ? "" : customer.getAddress().getGeolocatableAddress();
	}

	private String getHourOrAddress(final Order order, final Customer customer) {
		return order.getType() == OrderType.COLLECT ? getCollectionHour(order) : getAddress(customer);
	}

	public byte[] exportSaleCustomersToCsv(@NotNull final Integer saleYear, final OrderType orderType, @NotNull final String sorts)
			throws IOException {

		LOGGER.info("Exporting Customer Orders for Sale [{}] and Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);

		// get the plants
		final Set<Plant> plants = plantsService.getSalePlants(saleYear);

		final List<String> headers = new ArrayList<>(List.of(
				"#", "Name", "c/o", "Order Type", "Delivery Day", "Collection Slot", "Collection Hour", "Address", "Email Address", "Telephone"
		));
		plants.forEach(plant -> headers.add(plant.getName()));
		headers.add("# Plants");
		headers.add(String.format("Plant Price / %c", POUND_SIGN));
		headers.add(String.format("Discount / %c", POUND_SIGN));
		headers.add(String.format("Paid / %c", POUND_SIGN));
		headers.add(String.format("To Pay / %c", POUND_SIGN));

		final byte[] csv;
		try (final ByteArrayOutputStream baos = prepareCsvOutputStream();
			 final PrintWriter pw = new PrintWriter(baos, false, CSV_CHARSET);
			 final CSVPrinter csvPrinter = new CSVPrinter(pw, CSVFormat.EXCEL.builder().setHeader(headers.toArray(new String[0])).build())) {

			// get the orders
			final Set<Order> orders = ordersService.getSaleCustomerOrders(saleYear, orderType, sorts);

			// output each Order to the CSV
			for (final Order order : orders) {
				final Customer customer = order.getCustomer();

				final List<String> items = new ArrayList<>(List.of(
						Integer.toString(order.getNum()),
						customer.getName(),
						valueOrEmpty(order.getCourtesyOfName()),
						StringUtils.capitalize(order.getType().toString().toLowerCase()),
						StringUtils.capitalize(order.getDeliveryDay().toString().toLowerCase()),
						order.getCollectionSlot() == null ? "" : StringUtils.capitalize(order.getCollectionSlot().toString().toLowerCase()),
						getCollectionHour(order),
						getAddress(customer),
						valueOrEmpty(customer.getEmailAddress()),
						valueOrEmpty(customer.getTelephone())
				));

				addPlantAndPaymentDetails(items, plants, order);

				csvPrinter.printRecord(items);
			}

			csvPrinter.flush();
			csv = baos.toByteArray();
		}

		// return CSV content
		return csv;
	}

	public byte[] exportSaleCustomerPaymentsToCsv(@NotNull final Integer saleYear, final OrderType orderType, @NotNull final String sorts)
			throws IOException {

		LOGGER.info("Exporting Customer Order Payments for Sale [{}] and Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);

		final byte[] csv;
		try (final ByteArrayOutputStream baos = prepareCsvOutputStream();
			 final PrintWriter pw = new PrintWriter(baos, false, CSV_CHARSET);
			 final CSVPrinter csvPrinter = new CSVPrinter(pw, CSVFormat.EXCEL.builder().setHeader(
					 "#", "Name", "c/o", "Type", "Day", "Hour / Address", "Email Address", "Telephone", "# Plants",
					 String.format("Plant Price / %c", POUND_SIGN), String.format("Discount / %c", POUND_SIGN),
					 String.format("Paid / %c", POUND_SIGN), String.format("To Pay / %c", POUND_SIGN)
			 ).build())) {

			// get the orders
			final Set<Order> orders = ordersService.getSaleCustomerOrders(saleYear, orderType, sorts);

			// output each Order to the CSV
			for (final Order order : orders) {
				final Customer customer = order.getCustomer();

				csvPrinter.printRecord(List.of(
						Integer.toString(order.getNum()),
						customer.getName(),
						valueOrEmpty(order.getCourtesyOfName()),
						StringUtils.capitalize(order.getType().toString().toLowerCase()),
						StringUtils.capitalize(order.getDeliveryDay().toString().toLowerCase()),
						getHourOrAddress(order, customer),
						valueOrEmpty(customer.getEmailAddress()),
						valueOrEmpty(customer.getTelephone()),
						Integer.toString(order.getCount()),
						String.format("%.2f", order.getDisplayPrice()),
						order.getDiscount() == null ? "" : String.format("%.2f", order.getDisplayDiscount()),
						order.getPaid() == null ? "" : String.format("%.2f", order.getDisplayPaid()),
						order.getToPay() == 0 ? "0.00" : String.format("%.2f", order.getDisplayToPay())
				));
			}

			csvPrinter.flush();
			csv = baos.toByteArray();
		}

		// return CSV content
		return csv;
	}

	public byte[] exportAddressesToCsv() throws IOException {
		LOGGER.info("Exporting Customer Addresses (for all Sales)");

		final byte[] csv;
		try (final ByteArrayOutputStream baos = prepareCsvOutputStream();
			 final PrintWriter pw = new PrintWriter(baos, false, CSV_CHARSET);
			 final CSVPrinter csvPrinter = new CSVPrinter(pw, CSVFormat.EXCEL.builder().setHeader(
					 "House Name/Number", "Street", "Town", "City", "Postcode", "Address", "Last Order Year"
			 ).build())) {

			// get the sales
			final Map<Address, Integer> addressYears = new TreeMap<>(); // sort using the Address#compareTo definition
			final Set<Sale> sales = salesService.findAllSales();
			for (final Sale sale : sales) {
				final int saleYear = sale.getYear();
				for (final Customer customer : sale.getCustomers()) {
					final Address address = customer.getAddress();
					if (address != null && (!addressYears.containsKey(address) || addressYears.get(address) < saleYear)) {
						addressYears.put(address, saleYear);
					}
				}
			}

			// output each Address to the CSV
			for (final Map.Entry<Address, Integer> addressYear : addressYears.entrySet()) {
				final Address address = addressYear.getKey();
				csvPrinter.printRecord(List.of(
					valueOrEmpty(address.getHouseNameNumber()),
					valueOrEmpty(address.getStreet()),
					valueOrEmpty(address.getTown()),
					valueOrEmpty(address.getCity()),
					valueOrEmpty(address.getPostcode()),
					valueOrEmpty(address.getGeolocatableAddress()),
					Integer.toString(addressYear.getValue())
				));
			}

			csvPrinter.flush();
			csv = baos.toByteArray();
		}

		// return CSV content
		return csv;
	}

	private ByteArrayOutputStream prepareCsvOutputStream() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// write bom to prevent mis-coding
		baos.write(CSV_BOM);

		return baos;
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
