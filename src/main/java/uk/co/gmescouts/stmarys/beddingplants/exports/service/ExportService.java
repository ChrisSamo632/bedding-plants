package uk.co.gmescouts.stmarys.beddingplants.exports.service;

import com.google.maps.errors.ApiException;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.CompressionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public final class ExportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportService.class);

    private static final Charset CSV_CHARSET = StandardCharsets.UTF_8;
    private static final byte[] CSV_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final char POUND_SIGN = '£';

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

    ExportService() throws UnknownHostException {
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Exporting Customer Orders (to PDF) for Sale [{}] and Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);
        }

        // set up the URL
        final StringBuilder sb = new StringBuilder(100);
        sb.append("sorts=").append(sorts);
        if (orderType != null) {
            sb.append("&orderType=").append(orderType);
        }

        return executeSaleHtmlExport(saleYear, String.format("%s%s?%s", baseUri, ExportHtml.EXPORT_CUSTOMER_ORDERS_HTML, sb), PageSize.A4);
    }

    public byte[] exportSaleDeliveryRoutesToPdf(@NotNull final Integer saleYear, @NotNull final String sorts) throws IOException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Exporting Delivery Routes for Sale [{}] sorted by [{}]", saleYear, sorts);
        }

        return executeSaleHtmlExport(saleYear, String.format("%s%s?%s", baseUri, ExportHtml.EXPORT_DELIVERY_ROUTES_HTML, "sorts=" + sorts), PageSize.A4.rotate());
    }

    private byte[] executeSaleHtmlExport(@NotNull final Integer saleYear, @NotNull final String exportHtmlUrl, @NotNull final PageSize pageSize) throws IOException {
        final String exportHostUrl = getExportHostUrl();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Calling HTML Export URL [{}]", exportHtmlUrl);
        }

        // get the HTML via external call
        final String html = restTemplate.getForObject(exportHostUrl + exportHtmlUrl, String.class, saleYear);

        // convert HTML to PDF
        byte[] pdf;

        // converter properties (image/css locations)
        final ConverterProperties converterProperties = new ConverterProperties();
        converterProperties.setBaseUri(exportHostUrl);

        // writer properties (compression)
        final WriterProperties writerProperties = new WriterProperties();
        writerProperties.setCompressionLevel(CompressionConstants.BEST_COMPRESSION);

        // converter
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            final PdfDocument pdfDocument = new PdfDocument(new PdfWriter(baos, writerProperties));
            pdfDocument.setDefaultPageSize(pageSize);
            HtmlConverter.convertToPdf(html, pdfDocument, converterProperties);
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Exporting Customer Orders (to CSV) for Sale [{}] and Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);
        }

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
        try (ByteArrayOutputStream baos = prepareCsvOutputStream();
             PrintWriter pw = new PrintWriter(baos, false, CSV_CHARSET);
             CSVPrinter csvPrinter = new CSVPrinter(pw, CSVFormat.EXCEL.builder().setHeader(headers.toArray(new String[0])).get())) {

            // get the orders
            final Set<Order> orders = ordersService.getSaleCustomerOrders(saleYear, orderType, sorts);

            // output each Order to the CSV
            for (final Order order : orders) {
                final Customer customer = order.getCustomer();

                final List<String> items = new ArrayList<>(List.of(
                        Integer.toString(order.getNum()),
                        customer.getName(),
                        valueOrEmpty(order.getCourtesyOfName()),
                        StringUtils.capitalize(order.getType().toString().toLowerCase(Locale.ROOT)),
                        StringUtils.capitalize(order.getDeliveryDay().toString().toLowerCase(Locale.ROOT)),
                        order.getCollectionSlot() == null ? "" : StringUtils.capitalize(order.getCollectionSlot().toString().toLowerCase(Locale.ROOT)),
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Exporting Customer Order Payments for Sale [{}] and Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);
        }

        final byte[] csv;
        try (ByteArrayOutputStream baos = prepareCsvOutputStream();
             PrintWriter pw = new PrintWriter(baos, false, CSV_CHARSET);
             CSVPrinter csvPrinter = new CSVPrinter(pw, CSVFormat.EXCEL.builder().setHeader(
                     "#", "Name", "c/o", "Type", "Day", "Hour / Address", "Email Address", "Telephone", "# Plants",
                     String.format("Plant Price / %c", POUND_SIGN), String.format("Discount / %c", POUND_SIGN),
                     String.format("Paid / %c", POUND_SIGN), String.format("To Pay / %c", POUND_SIGN)
             ).get())) {

            // get the orders
            final Set<Order> orders = ordersService.getSaleCustomerOrders(saleYear, orderType, sorts);

            // output each Order to the CSV
            for (final Order order : orders) {
                final Customer customer = order.getCustomer();

                csvPrinter.printRecord(List.of(
                        Integer.toString(order.getNum()),
                        customer.getName(),
                        valueOrEmpty(order.getCourtesyOfName()),
                        StringUtils.capitalize(order.getType().toString().toLowerCase(Locale.ROOT)),
                        StringUtils.capitalize(order.getDeliveryDay().toString().toLowerCase(Locale.ROOT)),
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Exporting Customer Addresses (for all Sales)");
        }

        final byte[] csv;
        try (ByteArrayOutputStream baos = prepareCsvOutputStream();
             PrintWriter pw = new PrintWriter(baos, false, CSV_CHARSET);
             CSVPrinter csvPrinter = new CSVPrinter(pw, CSVFormat.EXCEL.builder().setHeader(
                     "House Name/Number", "Street", "Town", "City", "Postcode", "Address", "Last Order Year", "Has Email Address"
             ).get())) {

            // get the sales
            final Map<Address, AddressSummary> addressSummaries = new TreeMap<>(); // sort using the Address#compareTo definition
            final List<Sale> sales = salesService.findAllSales().stream()
                    .sorted(Comparator.comparingInt(Sale::getSaleYear).reversed()).toList();
            for (final Sale sale : sales) {
                final int saleYear = sale.getSaleYear();
                sale.getCustomers().stream().filter(c -> Objects.nonNull(c.getAddress())).forEach(c -> {
                    final Address address = c.getAddress();
                    final boolean hasEmailAddress = StringUtils.isNotBlank(c.getEmailAddress());
                    if (addressSummaries.containsKey(address)) {
                        addressSummaries.get(address).setEmailAddressPresent(hasEmailAddress);
                    } else {
                        addressSummaries.put(c.getAddress(), new AddressSummary(saleYear, hasEmailAddress));
                    }
                });
            }

            // output each Address to the CSV
            for (final Map.Entry<Address, AddressSummary> addressEntry : addressSummaries.entrySet()) {
                final Address address = addressEntry.getKey();
                final AddressSummary addressSummary = addressEntry.getValue();
                csvPrinter.printRecord(List.of(
                        valueOrEmpty(address.getHouseNameNumber()),
                        valueOrEmpty(address.getStreet()),
                        valueOrEmpty(address.getTown()),
                        valueOrEmpty(address.getCity()),
                        valueOrEmpty(address.getPostcode()),
                        valueOrEmpty(address.getGeolocatableAddress()),
                        Integer.toString(addressSummary.getLastSaleYear()),
                        Boolean.toString(addressSummary.isEmailAddressPresent())
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Generating Geolocated Points from Addresses for Sale Year [{}] and Order Type [{}]", saleYear, orderType);
        }

        final Set<Address> geolocatedAddresses = getSaleAddresses(saleYear, orderType, true);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] Geolocated Addresses", CollectionUtils.size(geolocatedAddresses));
        }

        Set<GeolocatedPoint> geolocatedPoints = Collections.emptySet();
        if (geolocatedAddresses != null) {
            // convert Addresses to GeolocatedPoints ready for plotting on the map
            geolocatedPoints = geolocatedAddresses.stream().map(ExportService::convertAddressToGeolocatedPoint).collect(Collectors.toSet());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[{}] Geolocated Points from Addresses", CollectionUtils.size(geolocatedPoints));
            }
        }

        return geolocatedPoints;
    }

    public byte[] exportGeolocatedSaleAddressesToImage(@NotNull final Integer saleYear, final OrderType orderType,
                                                       @NotNull final MapImageFormat mapImageFormat, @NotNull final MapType mapType) throws ApiException, InterruptedException, IOException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Generating Map Image for Addresses from Sale Year [{}] and Order Type [{}] in Format [{}] with Map Type [{}]", saleYear,
                    orderType, mapImageFormat, mapType);
        }

        // get the (geolocated) Addresses as Points
        final Set<GeolocatedPoint> geolocatedPoints = getGeolocatedSaleAddressesAsPoints(saleYear, orderType);

        // generate the image
        byte[] mapImg = null;
        if (CollectionUtils.isNotEmpty(geolocatedPoints)) {
            // get the image containing the Geolocated Points
            mapImg = geolocationService.plotPointsOnMapImage(geolocatedPoints, mapImageFormat, mapType);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Generated Image size [{}]", mapImg == null ? 0 : mapImg.length);
            }
        }

        return mapImg;
    }

    public Set<Address> getSaleAddresses(@NotNull final Integer saleYear, final OrderType orderType, final boolean geolocatedOnly) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Retrieving Addresses for Sale Year [{}] and Order Type [{}], Geolocated Only [{}]", saleYear, orderType, geolocatedOnly);
        }

        // get Addresses for specified Sale Year/OrderType
        Set<Address> addresses;
        if (orderType != null) {
            addresses = addressRepository.findAddressByCustomersSaleSaleYearAndCustomersOrdersType(saleYear, orderType);
        } else {
            addresses = addressRepository.findAddressByCustomersSaleSaleYear(saleYear);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] Sale Addresses", CollectionUtils.size(addresses));
        }

        // filter to geolocated addresses only if so requested
        if (addresses != null && geolocatedOnly) {
            // geolocate Address(es), if not already
            if (CollectionUtils.isNotEmpty(addresses)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Geolocating Sale Addresses (if not previously geolocated)");
                }
                addresses.forEach(this::geolocateAddress);

                // save these to the database
                addresses = new HashSet<>(addressRepository.saveAll(addresses));
            }

            addresses = addresses.stream().filter(Address::isGeolocated).collect(Collectors.toSet());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[{}] Geolocated Sale Addresses", CollectionUtils.size(addresses));
            }
        }

        return addresses;
    }

    private static GeolocatedPoint convertAddressToGeolocatedPoint(@NotNull final Address address) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Converting Address [{}] to Geolocated Point", address);
        }

        final GeolocatedPoint geolocatedPoint = new GeolocatedPoint(address.getGeolocation().getLatitude(), address.getGeolocation().getLongitude(),
                address.getGeolocatableAddress());

        // determine size of the marker based on number of orders
        // TODO: does this need restricting to specific Sale Year (as Customer may be a repeat across multiple Years)? Same for Order Types below?
        final long numOrders = address.getCustomers().stream().mapToLong(customer -> customer.getOrders().size()).sum();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Number of Orders associated with this Address [{}]", numOrders);
        }
        if (numOrders == 1) {
            geolocatedPoint.setMapMarkerSize(MapMarkerSize.NORMAL);
        } else {
            geolocatedPoint.setMapMarkerSize(MapMarkerSize.MID);
        }

        // determine colour of marker based on order type
        final boolean delivery = address.getCustomers().stream().flatMap(customer -> customer.getOrders().stream()).map(Order::getType)
                .anyMatch(OrderType::isDelivery);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Address contains a Delivery [{}]", delivery);
        }
        if (delivery) {
            geolocatedPoint.setMapMarkerColour(MapMarkerColour.RED);
        } else {
            geolocatedPoint.setMapMarkerColour(MapMarkerColour.GREEN);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Generated Geolocated Point [{}]", geolocatedPoint);
        }

        return geolocatedPoint;
    }

    private void geolocateAddress(@NotNull final Address address) {
        if (address.getGeolocation() == null && address.isGeolocatable()) {
            final String geolocatableAddress = address.getGeolocatableAddress();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Geolocatable Address [{}]", geolocatableAddress);
            }

            final Geolocation geolocation = geolocationService.geolocateGeolocatableAddress(geolocatableAddress);
            if (geolocation != null && StringUtils.isNoneBlank(geolocation.getFormattedAddress())) {
                address.setGeolocation(geolocation);
            }
        }
    }

    @lombok.Data
    @AllArgsConstructor
    private static class AddressSummary {
        Integer lastSaleYear;
        boolean emailAddressPresent;
    }
}
