package uk.co.gmescouts.stmarys.beddingplants.imports.service;

import com.poiji.bind.Poiji;
import com.poiji.exception.PoijiExcelType;
import com.poiji.option.PoijiOptions.PoijiOptionsBuilder;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import uk.co.gmescouts.stmarys.beddingplants.data.AddressRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Address;
import uk.co.gmescouts.stmarys.beddingplants.data.model.CollectionSlot;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryDay;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderItem;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.imports.configuration.ImportProperties;
import uk.co.gmescouts.stmarys.beddingplants.imports.model.excel.ExcelOrder;
import uk.co.gmescouts.stmarys.beddingplants.imports.model.excel.ExcelPlant;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Validated
public class ImportService {
    private final PoijiOptionsBuilder poijiOptionsBuilder;
    private final ImportProperties importProperties;
    private final SalesService salesService;
    private final AddressRepository addressRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportService.class);

    private static final String GROVE = " Grove";
    private static final Map<String, String> ADDRESS_CONTRACTIONS = Map.ofEntries(
            Map.entry(" st", " Street"),
            Map.entry(" ave", " Avenue"),
            Map.entry(" av", " Avenue"),
            Map.entry(" rd", " Road"),
            Map.entry(" dv", " Drive"),
            Map.entry(" cres", " Crescent"),
            Map.entry(" cl", " Close"),
            Map.entry(" ln", " Lane"),
            Map.entry(" terr", " Terrace"),
            Map.entry(" gv", GROVE),
            Map.entry(" grv", GROVE),
            Map.entry(" gr", GROVE)
    );

    private static final Map<Method, Method> IMPORT_METHOD_CACHE = new HashMap<>(100, 1);

    public ImportService(final PoijiOptionsBuilder poijiOptionsBuilder, final ImportProperties importProperties, final SalesService salesService,
                         final AddressRepository addressRepository) {
        this.poijiOptionsBuilder = poijiOptionsBuilder;
        this.importProperties = importProperties;
        this.salesService = salesService;
        this.addressRepository = addressRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Sale importSaleFromExcelFile(final MultipartFile file, final Integer saleYear, final Double vat, final Double deliveryCharge,
                                        final String orderImportsSheetName, final String plantImportsSheetName) throws IOException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Importing Sale from file [{}] for Order Year [{}] with VAT [{}] and Delivery Charge [{}]", file.getOriginalFilename(), saleYear, vat, deliveryCharge);
        }

        try (InputStream workbookInput = file.getInputStream(); Workbook workbook = WorkbookFactory.create(workbookInput)) {
            final List<ExcelPlant> importedPlants = readDataFromExcelFile(file, workbook,
                    StringUtils.defaultIfBlank(plantImportsSheetName, importProperties.plantImportsName()), ExcelPlant.class);
            final List<ExcelOrder> importedOrders = readDataFromExcelFile(file, workbook,
                    StringUtils.defaultIfBlank(orderImportsSheetName, importProperties.orderImportsName()), ExcelOrder.class);

            Sale sale = salesService.findSaleByYear(saleYear);
            if (sale == null) {
                sale = new Sale(saleYear, vat, deliveryCharge);
            }

            // save the Plants first so the Orders' items can reference them; the Customers are then added to the managed Sale
            // and persisted on flush (merging them would compute OrderItem ids before their Orders are linked)
            final Sale savedSale = salesService.saveSale(updateSaleWithImportedPlants(importedPlants, sale));
            return updateSaleWithImportedCustomers(importedOrders, savedSale);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Sale importCustomersFromExcel(final MultipartFile file, final String orderImportsSheetName, @NotNull final Integer saleYear)
            throws IOException {
        // get the Sale using the Year
        final Sale sale = salesService.findSaleByYear(saleYear);
        try (InputStream workbookInput = file.getInputStream(); Workbook workbook = WorkbookFactory.create(workbookInput)) {
            final List<ExcelOrder> importedOrders = readDataFromExcelFile(file, workbook,
                    StringUtils.defaultIfBlank(orderImportsSheetName, importProperties.orderImportsName()), ExcelOrder.class);
            return updateSaleWithImportedCustomers(importedOrders, sale);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Sale importPlantsFromExcel(final MultipartFile file, final String plantImportsSheetName, @NotNull final Integer saleYear)
            throws IOException {
        // get the Sale using the Year
        final Sale sale = salesService.findSaleByYear(saleYear);
        try (InputStream workbookInput = file.getInputStream(); Workbook workbook = WorkbookFactory.create(workbookInput)) {
            final List<ExcelPlant> importedPlants = readDataFromExcelFile(file, workbook,
                    StringUtils.defaultIfBlank(plantImportsSheetName, importProperties.plantImportsName()), ExcelPlant.class);
            return salesService.saveSale(updateSaleWithImportedPlants(importedPlants, sale));
        }
    }

    private Sale updateSaleWithImportedCustomers(final List<ExcelOrder> importedOrders, @NotNull final Sale sale) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Importing Orders for Sale [{}]", sale.getSaleYear());
        }

        // check the Sale contains some Plants
        final Set<Plant> plants = sale.getPlants();
        if (plants.isEmpty()) {
            throw new IllegalArgumentException(String.format("Cannot import Orders for a Sale without Plants [%d]", sale.getSaleYear()));
        }

        // per-import Address lookup, pre-seeded with any Addresses already stored
        @SuppressWarnings("null")
        final List<ExcelOrder> validOrders = importedOrders.stream().filter(ExcelOrder::isValid).toList();
        final Map<String, Address> addresses = loadExistingAddresses(validOrders);

        // convert to Orders
        final List<Customer> customerOrders = validOrders.stream().map(order -> createCustomer(order, sale, plants, addresses)).toList();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Imported [{}] valid Customer Orders", customerOrders.size());
        }

        // merge duplicate Customers and aggregate Orders
        final Set<Customer> customers = new HashSet<>();
        customerOrders.forEach(customerOrder -> {
            if (customers.contains(customerOrder)) {
                final Customer existingCustomer = customers.stream().filter(customerOrder::equals).findFirst()
                        .orElseThrow(IllegalStateException::new);
                customerOrder.getOrders().forEach(existingCustomer::addOrder);
            } else {
                customers.add(customerOrder);
            }
        });
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Imported [{}] de-duplicated Customers", customers.size());
        }

        customers.forEach(sale::addCustomer);
        return sale;
    }

    private Sale updateSaleWithImportedPlants(final List<ExcelPlant> importedPlants, @NotNull final Sale sale) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Importing Plants for Sale [{}]", sale.getSaleYear());
        }

        @SuppressWarnings("null")
        final Set<Plant> plants = importedPlants.stream().filter(ExcelPlant::isValid).map(this::createPlant).collect(Collectors.toSet());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Imported [{}] valid Plants", plants.size());
        }

        plants.forEach(sale::addPlant);
        return sale;
    }

    private Plant createPlant(final ExcelPlant excelPlant) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Convert imported Plant: [{}]", excelPlant);
        }

        final Double price = StringUtils.isNotBlank(excelPlant.price()) ? Double.parseDouble(excelPlant.price().replaceFirst("£", "")) : 0d;
        final Double cost = StringUtils.isNotBlank(excelPlant.cost()) ? Double.parseDouble(excelPlant.cost().replaceFirst("£", "")) : 0d;

        final Plant plant = new Plant(Integer.valueOf(excelPlant.id()), excelPlant.name(), excelPlant.variety(), price, cost);
        plant.setDetails(excelPlant.details());
        return plant;
    }

    private Customer createCustomer(final ExcelOrder excelOrder, final Sale sale, final Set<Plant> plants,
                                  final Map<String, Address> addresses) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Convert imported Order: [{}]", excelOrder);
        }

        // create Customer (without Address), linked to the Sale up front as the Customer id depends on the Sale year
        final Customer customer = new Customer(excelOrder.forename(), excelOrder.surname());
        customer.setSale(sale);
        customer.setEmailAddress(excelOrder.emailAddress());
        customer.setTelephone(normaliseTelephoneNumber(excelOrder.telephone()));

        // add Order
        final Order order = createOrder(excelOrder, sale, plants);
        customer.addOrder(order);

        // link Customer with Address (if present)
        final Address address = createAddress(excelOrder, addresses);
        if (address != null) {
            address.addCustomer(customer);
        }

        return customer;
    }

    private Order createOrder(final ExcelOrder excelOrder, final Sale sale, @NotNull @Size(min = 1) final Set<Plant> plants) {
        // determine DeliveryDay (default is Saturday if not present)
        final DeliveryDay deliveryDay = StringUtils.isNotBlank(excelOrder.deliveryDay())
                ? DeliveryDay.valueOf(StringUtils.upperCase(excelOrder.deliveryDay()))
                : DeliveryDay.SATURDAY;

        // determine CollectionSlot (default is Any if not present)
        final CollectionSlot collectionSlot = StringUtils.isNotBlank(excelOrder.collectionSlot())
                ? CollectionSlot.valueOf(StringUtils.upperCase(excelOrder.collectionSlot()))
                : CollectionSlot.ANY;

        final DeliveryRoute deliveryRoute = StringUtils.isNotBlank(excelOrder.deliveryRoute())
                ? getOrCreateImportedDeliveryRoute(Long.parseLong(excelOrder.deliveryRoute()), deliveryDay, sale)
                : null;

        // create Order (without Customer or OrderItems)
        final Order order = new Order(Integer.valueOf(excelOrder.orderNumber()), deliveryDay, OrderType.valueOf(excelOrder.collectDeliver().toUpperCase(Locale.ROOT).charAt(0)));
        order.setDeliveryRoute(deliveryRoute);
        order.setCollectionSlot(collectionSlot);
        order.setCollectionHour(StringUtils.isNotBlank(excelOrder.collectionHour()) ? Integer.parseInt(excelOrder.collectionHour()) : null);
        order.setCourtesyOfName(excelOrder.courtesyOf());
        order.setSurvey(excelOrder.survey());
        order.setNotes(excelOrder.notes());
        order.setPaid(StringUtils.isNotBlank(excelOrder.paid()) ? Double.valueOf(excelOrder.paid()) : null);
        order.setDiscount(StringUtils.isNotBlank(excelOrder.discount()) ? Double.valueOf(excelOrder.discount()) : null);

        if (deliveryRoute != null) {
            deliveryRoute.addOrder(order);
        }

        // determine requested number of each plant and create OrderItems on the Order
        for (final Plant plant : plants) {
            final int plantId = plant.getNum();
            try {
                // for each available (imported) Plant, check how many the imported order wants
                final String plantCountStr = (String) excelOrder.getClass().getMethod(String.format("numberPlants%d", plantId)).invoke(excelOrder);

                final int numPlants = StringUtils.isNotBlank(plantCountStr) ? Integer.parseInt(plantCountStr) : 0;

                if (numPlants > 0) {
                    // add the OrderItem to the Order
                    order.addOrderItem(new OrderItem(plant, numPlants));
                }
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException |
                     InvocationTargetException e) {
                throw new IllegalStateException(String.format("Unable to determine requested number of plants [%d]: %s", plantId, e.getMessage()), e);
            }
        }

        return order;
    }

    private DeliveryRoute getOrCreateImportedDeliveryRoute(final long routeNumber, final DeliveryDay deliveryDay, final Sale sale) {
        final DeliveryRoute existingRoute = sale.getDeliveryRoutes().stream()
                .filter(route -> routeNumber == route.getNum()).findFirst().orElse(null);
        if (existingRoute != null) {
            if (existingRoute.getDeliveryDay() != deliveryDay) {
                throw new IllegalArgumentException(String.format("Delivery route [%d] is assigned to [%s], not [%s]",
                        routeNumber, existingRoute.getDeliveryDay(), deliveryDay));
            }
            return existingRoute;
        }

        final DeliveryRoute newRoute = new DeliveryRoute(routeNumber, deliveryDay);
        newRoute.setSale(sale);
        sale.addDeliveryRoute(newRoute);
        return newRoute;
    }

    String normaliseTelephoneNumber(final String telephone) {
        String normalised = null;

        // normalise the telephone format, e.g. "0161 370 3070", "07867 123 456"
        if (StringUtils.isNotBlank(telephone)) {
            // remove any existing spaces
            final StringBuilder telephoneBuilder = new StringBuilder(telephone.replaceAll("\\s+", ""));

            // 7 digits -> local landline, prepend "0161" (4-3-4)
            if (telephoneBuilder.length() == 7) {
                telephoneBuilder.insert(0, "0161");
            }
            // add leading "0" if not present
            else if (!telephone.startsWith("0")) {
                telephoneBuilder.insert(0, "0");
            }

            // 11 digits -> mobile/landline (4-3-4)
            if (telephoneBuilder.length() == 11) {
                telephoneBuilder.insert(7, " ").insert(4, " ");
            }
            // other -> unknown,
            else {
                // just add a space somewhere in the middle so leading 0 digits aren't lost if re-imported to Excel
                telephoneBuilder.insert(telephone.length() / 2, " ");
            }
            normalised = telephoneBuilder.toString();
        }

        return normalised;
    }

    private Map<String, Address> loadExistingAddresses(final List<ExcelOrder> orders) {
        @SuppressWarnings("null")
        final Set<String> ids = orders.stream().map(this::buildAddress).filter(java.util.Objects::nonNull).map(Address::getId)
                .collect(Collectors.toSet());
        final Map<String, Address> addresses = new HashMap<>();
        addressRepository.findAllById(ids).forEach(address -> addresses.put(address.getId(), address));
        return addresses;
    }

    private Address createAddress(final ExcelOrder excelOrder, final Map<String, Address> addresses) {
        final Address address = buildAddress(excelOrder);
        return address == null ? null : addresses.computeIfAbsent(address.getId(), id -> address);
    }

    private Address buildAddress(final ExcelOrder excelOrder) {
        String street = excelOrder.street();
        if (StringUtils.isNotBlank(excelOrder.street())) {
            street = ADDRESS_CONTRACTIONS.entrySet().stream()
                    // find if any contractions match the end of the imported street
                    .filter(contraction -> excelOrder.street().toLowerCase(Locale.ROOT).endsWith(contraction.getKey()))
                    // replace the contraction with the full street ending
                    .map(contraction -> excelOrder.street().replaceFirst(String.format("%s$", contraction.getKey()), contraction.getValue()))
                    // there can be only one (or none, in which case stick with the original value)...
                    .findFirst().orElse(street);
        }

        // normalise postcode
        final StringBuilder postcodeBuilder = new StringBuilder(10);
        final String importedPostcode = excelOrder.postcode();
        if (importedPostcode != null) {
            postcodeBuilder.append(importedPostcode.replaceAll("\\s+", ""));
            postcodeBuilder.insert(postcodeBuilder.length() > 3 ? postcodeBuilder.length() - 3 : 0, " ");
        }

        final Address address = new Address();
        address.setHouseNameNumber(excelOrder.houseNameNumber());
        address.setStreet(street);
        address.setTown(excelOrder.town());
        address.setPostcode(postcodeBuilder.toString());

        // if there's something in the Address, add the City for Geolocation and return
        if (address.isGeolocatable()) {
            address.setCity(StringUtils.defaultIfEmpty(excelOrder.city(), importProperties.defaultCity()));

            return address;
        }

        // if Address unusable, don't return anything
        return null;
    }

    String normaliseField(final String fieldName, final String field) {
        // convert blank strings to nulls
        String normalised = null;

        if (StringUtils.isNotBlank(field) && !StringUtils.isAsciiPrintable(StringUtils.replaceEach(field, new String[]{"£", "\n", "\r"}, new String[]{"", " ", ""}))) {
            throw new IllegalArgumentException(String.format("Field [%s] value must be ASCII printable: %s", fieldName, field));
        }

        // trim multiple spaces (anywhere in a String)
        if (StringUtils.isNotBlank(field)) {
            normalised = field.replaceAll("\\s{2,}", " ");
        }

        return normalised;
    }

    private Method findSetter(final Object imported, final Method getter) {
        Method setter = IMPORT_METHOD_CACHE.get(getter);

        if (setter == null) {
            final String setterName = getter.getName().replaceFirst("^get", "set");
            try {
                setter = imported.getClass().getMethod(setterName, String.class);
                IMPORT_METHOD_CACHE.put(getter, setter);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(String.format("Unable to find setter [%s] for imported object [%s]: %s", setterName,
                        imported.getClass().getSimpleName(), e.getMessage()), e);
            }
        }

        return setter;
    }

    private <T> void normaliseImportedFields(final T imported) {
        final AtomicInteger row = new AtomicInteger(1);

        Arrays.stream(imported.getClass().getDeclaredMethods())
                // find the getter methods on the import object (public accessible returning Strings)
                .filter(method -> method.getName().startsWith("get") && method.canAccess(imported) && String.class.equals(method.getReturnType()))
                // normalise the value and set back on the object
                .forEach(getter -> {
                    final int r = row.getAndIncrement();
                    try {
                        // get original value
                        final String str = (String) getter.invoke(imported);

                        // normalise the value
                        final String normalised = normaliseField(getter.getName(), str);

                        // find the equivalent setter method on the imports object
                        final Method setter = findSetter(imported, getter);

                        setter.invoke(imported, normalised);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        throw new IllegalStateException(String.format("Unable to normalise field value [%s] for imported object [%s], row [%d]: %s",
                                getter.getName().replaceFirst("^get", ""), imported.getClass().getSimpleName(), r, e.getMessage()), e);
                    }
                });
    }

    private <T> List<T> readDataFromExcelFile(final InputStream inputStream, final Workbook workbook, final String sheetName, final Class<T> dataType) {
        // determine Excel Type (if valid)
        final PoijiExcelType excelType = getPoijiExcelType(workbook);

        // get sheet index from name
        final int index = getSheetIndexByName(workbook, sheetName);

        // read the data from the file
        final List<T> data = Poiji.fromExcel(inputStream, excelType, dataType, poijiOptionsBuilder.sheetIndex(index).build());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Read [{}] records of type [{}]", data.size(), dataType.getSimpleName());
        }

        // normalise all fields for each imported datum
        data.forEach(this::normaliseImportedFields);

        return data;
    }

    private <T> List<T> readDataFromExcelFile(final MultipartFile file, final Workbook workbook, final String sheetName, final Class<T> dataType)
            throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return readDataFromExcelFile(inputStream, workbook, sheetName, dataType);
        }
    }

    private PoijiExcelType getPoijiExcelType(final Workbook workbook) {
        PoijiExcelType poijiExcelType;

        final SpreadsheetVersion sv = workbook.getSpreadsheetVersion();
        poijiExcelType = switch (sv) {
            case EXCEL2007 -> PoijiExcelType.XLSX;
            case EXCEL97 -> PoijiExcelType.XLS;
        };
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Workbook POI Excel Type [{}]", poijiExcelType);
        }

        return poijiExcelType;
    }

    private int getSheetIndexByName(final Workbook workbook, final String name) {
        final int index = workbook.getSheetIndex(name);

        if (index < 0) {
            throw new IllegalStateException(String.format("Cannot locate worksheet with name %s", name));
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Worksheet [{}] Index [{}]", name, index);
        }

        return index;
    }
}
