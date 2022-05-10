package uk.co.gmescouts.stmarys.beddingplants.exports;

import com.google.maps.errors.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Address;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.exports.service.ExportService;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapImageFormat;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapType;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping(value = "/export")
class Export {
	private static final Logger LOGGER = LoggerFactory.getLogger(Export.class);

	/*
	 * Orders
	 */
	private static final String EXPORT_CUSTOMER_ORDERS = "/orders/{saleYear}";
	private static final String EXPORT_CUSTOMER_ORDERS_PDF = EXPORT_CUSTOMER_ORDERS + "/pdf";
	private static final String EXPORT_CUSTOMER_ORDERS_CSV = EXPORT_CUSTOMER_ORDERS + "/csv";

	/*
     * Payments
	 */
	private static final String EXPORT_CUSTOMER_ORDER_PAYMENTS = "/payments/{saleYear}";
	private static final String EXPORT_CUSTOMER_ORDER_PAYMENTS_CSV = EXPORT_CUSTOMER_ORDER_PAYMENTS + "/csv";

	/*
	 * Addresses
	 */
	private static final String EXPORT_CUSTOMER_ADDRESSES = "/addresses";
	private static final String EXPORT_CUSTOMER_ADDRESSES_YEAR = "/addresses/{saleYear}";
	private static final String EXPORT_CUSTOMER_ADDRESSES_IMG = EXPORT_CUSTOMER_ADDRESSES_YEAR + "/img";

	/*
	 * Delivery Routes
	 */
	private static final String EXPORT_DELIVERY_ROUTES_PDF = "/routes/{saleYear}/pdf";

	private static final MediaType MEDIA_TYPE_TEXT_CSV = MediaType.parseMediaType("text/csv");

	@Resource
	private ExportService exportService;

	@GetMapping(produces = MediaType.APPLICATION_PDF_VALUE, value = EXPORT_CUSTOMER_ORDERS_PDF)
	public ResponseEntity<ByteArrayResource> exportSaleCustomerOrdersAsPdf(@PathVariable final Integer saleYear,
																		   @RequestParam(required = false) final OrderType orderType,
																		   @RequestParam(defaultValue = "type:DESC,deliveryDay:ASC,deliveryRoute.num:ASC,collectionHour:ASC,num:ASC") final String sorts)
			throws IOException {

		LOGGER.info("Exporting (PDF) Order details for Sale [{}] with Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);

		// get the PDF content
		final byte[] pdf = exportService.exportSaleCustomersToPdf(saleYear, orderType, sorts);

		if (pdf == null) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.ok().headers(getNoCacheHeaders(String.format("attachment; filename=\"sale_orders_%s.pdf\"", saleYear)))
				.contentLength(pdf.length).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new ByteArrayResource(pdf));
	}

	@GetMapping(produces = "text/csv", value = EXPORT_CUSTOMER_ORDERS_CSV)
	public ResponseEntity<ByteArrayResource> exportSaleCustomerOrdersAsCsv(@PathVariable final Integer saleYear,
																		   @RequestParam(required = false) final OrderType orderType,
																		   @RequestParam(defaultValue = "type:DESC,deliveryDay:ASC,collectionHour:ASC,num:ASC") final String sorts)
			throws IOException {

		LOGGER.info("Exporting (CSV) Order details for Sale [{}] with Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);

		// get the CSV content
		final byte[] csv = exportService.exportSaleCustomersToCsv(saleYear, orderType, sorts);

		if (csv == null) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.ok().headers(getNoCacheHeaders(String.format("attachment; filename=\"sale_orders_%s.csv\"", saleYear)))
				.contentLength(csv.length).contentType(MEDIA_TYPE_TEXT_CSV).body(new ByteArrayResource(csv));
	}

	@GetMapping(produces = "text/csv", value = EXPORT_CUSTOMER_ORDER_PAYMENTS_CSV)
	public ResponseEntity<ByteArrayResource> exportSaleCustomerPaymentsToCsv(@PathVariable final Integer saleYear,
																		     @RequestParam(required = false) final OrderType orderType,
																		     @RequestParam(defaultValue = "type:DESC,deliveryDay:ASC,collectionHour:ASC,num:ASC") final String sorts)
			throws IOException {

		LOGGER.info("Exporting (CSV) Order details for Sale [{}] with Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);

		// get the CSV content
		final byte[] csv = exportService.exportSaleCustomerPaymentsToCsv(saleYear, orderType, sorts);

		if (csv == null) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.ok().headers(getNoCacheHeaders(String.format("attachment; filename=\"sale_order_payments_%s.csv\"", saleYear)))
				.contentLength(csv.length).contentType(MEDIA_TYPE_TEXT_CSV).body(new ByteArrayResource(csv));
	}

	@GetMapping(EXPORT_CUSTOMER_ADDRESSES_YEAR)
	public Set<Address> exportSaleAddressesAsJson(@PathVariable final Integer saleYear,
												  @RequestParam(required = false) final OrderType orderType,
												  @RequestParam(required = false, defaultValue = "false") final boolean geolocatedOnly) {
		LOGGER.info("Exporting (JSON); Addresses for Sale [{}] with Order Type [{}] and Geolocated [{}]", saleYear, orderType, geolocatedOnly);

		// get the Addresses
		return exportService.getSaleAddresses(saleYear, orderType, geolocatedOnly);
	}

	@GetMapping(EXPORT_CUSTOMER_ADDRESSES)
	public ResponseEntity<ByteArrayResource> exportAddresses() throws IOException {
		LOGGER.info("Exporting (CSV); Addresses with last Sale Year");

		// get the Addresses
		final byte[] csv = exportService.exportAddressesToCsv();

		if (csv == null) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.ok().headers(getNoCacheHeaders("attachment; filename=\"address_sales.csv\""))
				.contentLength(csv.length).contentType(MEDIA_TYPE_TEXT_CSV).body(new ByteArrayResource(csv));
	}

	@GetMapping(EXPORT_CUSTOMER_ADDRESSES_IMG)
	public ResponseEntity<ByteArrayResource> exportSaleAddressesAsImage(final Model model,
																		@PathVariable final Integer saleYear,
																		@RequestParam(required = false) final OrderType orderType,
																		@RequestParam(defaultValue = "PNG") final MapImageFormat mapImageFormat,
																		@RequestParam(defaultValue = "ROADMAP") final MapType mapType)
			throws ApiException, InterruptedException, IOException {

		LOGGER.info("Exporting (IMG); Addresses for Sale [{}] with Order Type [{}]", saleYear, orderType);

		// get the image
		final byte[] mapImg = exportService.exportGeolocatedSaleAddressesToImage(saleYear, orderType, mapImageFormat, mapType);

		if (mapImg == null) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.ok()
				.headers(getNoCacheHeaders(String.format("attachment; filename=\"map_sale_orders_%s%s.%s\"", saleYear,
						(orderType == null ? "" : orderType), mapImageFormat.getFilenameExtension())))
				.contentLength(mapImg.length).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new ByteArrayResource(mapImg));
	}

	@GetMapping(produces = MediaType.APPLICATION_PDF_VALUE, value = EXPORT_DELIVERY_ROUTES_PDF)
	public ResponseEntity<ByteArrayResource> exportSaleDeliveryRoutesAsPdf(@PathVariable final Integer saleYear,
																		   @RequestParam(defaultValue = "day:ASC,num:ASC") final String sorts)
			throws IOException {

		LOGGER.info("Exporting (PDF) Delivery Route details for Sale [{}] sorted by [{}]", saleYear, sorts);

		// get the PDF content
		final byte[] pdf = exportService.exportSaleDeliveryRoutesToPdf(saleYear, sorts);

		if (pdf == null) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.ok().headers(getNoCacheHeaders(String.format("attachment; filename=\"sale_delivery_routes_%s.pdf\"", saleYear)))
				.contentLength(pdf.length).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new ByteArrayResource(pdf));
	}

	private static HttpHeaders getNoCacheHeaders(final String contentDisposition) {
		final HttpHeaders headers = new HttpHeaders();

		headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
		headers.add(HttpHeaders.PRAGMA, "no-cache");
		headers.add(HttpHeaders.EXPIRES, "0");

		if (contentDisposition != null) {
			headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
		}

		return headers;
	}
}
