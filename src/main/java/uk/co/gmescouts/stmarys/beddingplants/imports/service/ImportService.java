package uk.co.gmescouts.stmarys.beddingplants.imports.service;

import com.poiji.bind.Poiji;
import com.poiji.exception.PoijiExcelType;
import com.poiji.option.PoijiOptions.PoijiOptionsBuilder;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
import uk.co.gmescouts.stmarys.beddingplants.deliveries.service.DeliveriesService;
import uk.co.gmescouts.stmarys.beddingplants.imports.configuration.ImportConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.imports.model.excel.ExcelOrder;
import uk.co.gmescouts.stmarys.beddingplants.imports.model.excel.ExcelPlant;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ImportService {
	@Resource
	private PoijiOptionsBuilder poijiOptionsBuilder;

	@Resource
	private ImportConfiguration importConfiguration;

	@Resource
	private SalesService salesService;

	@Resource
	private DeliveriesService deliveriesService;

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportService.class);

	private static final String GROVE = " Grove";
	private static final Map<String, String> ADDRESS_CONTRACTIONS;
	static {
		final Map<String, String> contractions = new HashMap<>(15, 1);
		contractions.put(" st", " Street");
		contractions.put(" ave", " Avenue");
		contractions.put(" av", " Avenue");
		contractions.put(" rd", " Road");
		contractions.put(" dv", " Drive");
		contractions.put(" cres", " Crescent");
		contractions.put(" cl", " Close");
		contractions.put(" ln", " Lane");
		contractions.put(" terr", " Terrace");

		contractions.put(" gv", GROVE);
		contractions.put(" grv", GROVE);
		contractions.put(" gr", GROVE);

		ADDRESS_CONTRACTIONS = Collections.unmodifiableMap(contractions);
	}

	private static final Map<Method, Method> IMPORT_METHOD_CACHE = new HashMap<>(100, 1);

	private static final Map<Address, Address> IMPORTED_ADDRESS_CACHE = new HashMap<>(500, 1);

	public Sale importSaleFromExcelFile(final MultipartFile file, final Integer saleYear, final Double vat, final Double deliveryCharge,
										final String orderImportsSheetName, final String plantImportsSheetName) throws IOException {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Importing Sale from file [{}] for Order Year [{}] with VAT [{}] and Delivery Charge [{}]", file.getOriginalFilename(), saleYear, vat, deliveryCharge);
		}

		// check for existing Sale for the specified year
		Sale sale = salesService.findSaleByYear(saleYear);
		if (sale == null) {
			// create new Sale if it doesn't exist
			sale = salesService.saveSale(Sale.builder().saleYear(saleYear).vat(vat).deliveryCharge(deliveryCharge).build());
		}

		// import Plants (and add to Sale)
		sale = updateSaleWithImportedPlantsFromExcel(file, plantImportsSheetName, sale);

		// import Orders (and add to Sale)
		sale = updateSaleWithImportedCustomersFromExcel(file, orderImportsSheetName, sale);

		// return the Sale
		return salesService.saveSale(sale);
	}

	public Sale importCustomersFromExcel(final MultipartFile file, final String orderImportsSheetName, @NotNull final Integer saleYear)
			throws IOException {
		// get the Sale using the Year
		final Sale sale = salesService.findSaleByYear(saleYear);

		return updateSaleWithImportedCustomersFromExcel(file, orderImportsSheetName, sale);
	}

	public Sale importPlantsFromExcel(final MultipartFile file, final String plantImportsSheetName, @NotNull final Integer saleYear)
			throws IOException {
		// get the Sale using the Year
		final Sale sale = salesService.findSaleByYear(saleYear);

		// return the updated Sale
		return updateSaleWithImportedPlantsFromExcel(file, plantImportsSheetName, sale);
	}

	private Sale updateSaleWithImportedCustomersFromExcel(final MultipartFile file, final String orderImportsSheetName, @NotNull final Sale sale)
			throws IOException {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Importing Orders from file [{}] for Sale [{}]", file.getOriginalFilename(), sale.getSaleYear());
		}

		// check the Sale contains some Plants
		final Set<Plant> plants = sale.getPlants();
		if (plants.isEmpty()) {
			throw new IllegalArgumentException(String.format("Cannot import Orders for a Sale without Plants [%d]", sale.getSaleYear()));
		}

		// get Workbook from file (ensure we can read it and determine the type)
		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

			// Order Imports
			final List<ExcelOrder> importedOrders = readDataFromExcelFile(file.getInputStream(), workbook,
					StringUtils.defaultIfBlank(orderImportsSheetName, importConfiguration.getOrderImportsName()), ExcelOrder.class);

			// prepare address cache for import
			// TODO: surely there's a JPA way of handling this though, right?
			IMPORTED_ADDRESS_CACHE.clear();

			// convert to Orders
			final List<Customer> customerOrders = importedOrders.stream().filter(ExcelOrder::isValid).map(order -> createCustomer(order, sale, plants)).toList();
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Imported [{}] valid Customer Orders", customerOrders.size());
			}

			// merge duplicate Customers and aggregate Orders
			final Set<Customer> customers = new HashSet<>();
			customerOrders.forEach(customerOrder -> {
				// is Customer already present?
				if (customers.contains(customerOrder)) {
					final Customer existingCustomer = customers.stream().filter(customerOrder::equals).findFirst()
							.orElseThrow(IllegalStateException::new);

					// add new Order(s) to existing Customer
					customerOrder.getOrders().forEach(existingCustomer::addOrder);
				} else {
					// add new Customer with their Order
					customers.add(customerOrder);
				}
			});
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Imported [{}] de-duplicated Customers", customers.size());
			}

			// TODO is the "JPA way" to save the Customers (and the Plants) separately and then "refresh" the Sale instead?
			// add Customer to Sale
			// done as a second loop to avoid confusing the Customer#equals when looking for duplicates above
			customers.forEach(sale::addCustomer);

			// save any Sale updates
			return salesService.saveSale(sale);
		}
	}

	private Sale updateSaleWithImportedPlantsFromExcel(final MultipartFile file, final String plantImportsSheetName, @NotNull Sale sale)
			throws IOException {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Importing Plants from file [{}] for Sale [{}]", file.getOriginalFilename(), sale.getSaleYear());
		}

		// get Workbook from file (ensure we can read it and determine the type)
		try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
			// Plant Imports
			final List<ExcelPlant> importedPlants = readDataFromExcelFile(file.getInputStream(), workbook,
					StringUtils.defaultIfBlank(plantImportsSheetName, importConfiguration.getPlantImportsName()), ExcelPlant.class);

			// convert to Plants
			final Set<Plant> plants = importedPlants.stream().filter(ExcelPlant::isValid).map(this::createPlant).collect(Collectors.toSet());
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Imported [{}] valid Plants", plants.size());
			}

			// add to Sale
			plants.forEach(sale::addPlant);

			// save any Sale updates
			sale = salesService.saveSale(sale);
		}

		// return the updated Sale
		return sale;
	}

	private Plant createPlant(final ExcelPlant excelPlant) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Convert imported Plant: [{}]", excelPlant);
		}

		final Double price = StringUtils.isNotBlank(excelPlant.getPrice()) ? Double.parseDouble(excelPlant.getPrice().replaceFirst("£", "")) : 0d;
		final Double cost = StringUtils.isNotBlank(excelPlant.getCost()) ? Double.parseDouble(excelPlant.getCost().replaceFirst("£", "")) : 0d;

		return Plant.builder().num(Integer.valueOf(excelPlant.getId())).name(excelPlant.getName()).variety(excelPlant.getVariety())
				.details(excelPlant.getDetails()).price(price).cost(cost).build();
	}

	private Customer createCustomer(final ExcelOrder excelOrder, final Sale sale, final Set<Plant> plants) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Convert imported Order: [{}]", excelOrder);
		}

		// create Customer (without Address)
		final Customer customer = Customer.builder().forename(excelOrder.getForename()).surname(excelOrder.getSurname())
				.emailAddress(excelOrder.getEmailAddress()).telephone(normaliseTelephoneNumber(excelOrder.getTelephone())).build();

		// add Order
		final Order order = createOrder(excelOrder, sale, plants);
		customer.addOrder(order);

		// link Customer with Address (if present)
		final Address address = createAddress(excelOrder);
		if (address != null) {
			address.addCustomer(customer);
		}

		return customer;
	}

	private Order createOrder(final ExcelOrder excelOrder, final Sale sale, @NotNull @Size(min = 1) final Set<Plant> plants) {
		// determine DeliveryDay (default is Saturday if not present)
		final DeliveryDay deliveryDay = StringUtils.isNotBlank(excelOrder.getDeliveryDay())
				? DeliveryDay.valueOf(StringUtils.upperCase(excelOrder.getDeliveryDay()))
				: DeliveryDay.SATURDAY;

		// determine CollectionSlot (default is Any if not present)
		final CollectionSlot collectionSlot = StringUtils.isNotBlank(excelOrder.getCollectionSlot())
				? CollectionSlot.valueOf(StringUtils.upperCase(excelOrder.getCollectionSlot()))
				: CollectionSlot.ANY;

		// Delivery Route (default is none)
		final DeliveryRoute deliveryRoute = StringUtils.isNotBlank(excelOrder.getDeliveryRoute())
				? deliveriesService.getOrCreateDeliveryRoute(Long.parseLong(excelOrder.getDeliveryRoute()), deliveryDay, sale)
				: null;

		// create Order (without Customer or OrderItems)
		final Order order = Order.builder()
				.num(Integer.valueOf(excelOrder.getOrderNumber()))
				.type(OrderType.valueOf(excelOrder.getCollectDeliver().toUpperCase(Locale.ROOT).charAt(0)))
				.deliveryDay(deliveryDay)
				.deliveryRoute(deliveryRoute)
				.collectionSlot(collectionSlot)
				.collectionHour(StringUtils.isNotBlank(excelOrder.getCollectionHour()) ? Integer.parseInt(excelOrder.getCollectionHour()) : null)
				.courtesyOfName(excelOrder.getCourtesyOf())
				.notes(excelOrder.getNotes())
				.paid(StringUtils.isNotBlank(excelOrder.getPaid()) ? Double.valueOf(excelOrder.getPaid()) : null)
				.discount(StringUtils.isNotBlank(excelOrder.getDiscount()) ? Double.valueOf(excelOrder.getDiscount()) : null)
				.build();

		// determine requested number of each plant and create OrderItems on the Order
		for (final Plant plant : plants) {
			final int plantId = plant.getNum();
			try {
				// for each available (imported) Plant, check how many the imported order wants
				final String plantCountStr = (String) excelOrder.getClass().getMethod(String.format("getNumberPlants%d", plantId)).invoke(excelOrder);

				final int numPlants = StringUtils.isNotBlank(plantCountStr) ? Integer.parseInt(plantCountStr) : 0;

				if (numPlants > 0) {
					// add the OrderItem to the Order
					order.addOrderItem(OrderItem.builder().plant(plant).count(numPlants).build());
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IllegalStateException(String.format("Unable to determine requested number of plants [%d]: %s", plantId, e.getMessage()), e);
			}
		}

		return order;
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

	private Address createAddress(final ExcelOrder excelOrder) {
		String street = excelOrder.getStreet();
		if (StringUtils.isNotBlank(excelOrder.getStreet())) {
			street = ADDRESS_CONTRACTIONS.entrySet().stream()
					// find if any contractions match the end of the imported street
					.filter(contraction -> excelOrder.getStreet().toLowerCase(Locale.ROOT).endsWith(contraction.getKey()))
					// replace the contraction with the full street ending
					.map(contraction -> excelOrder.getStreet().replaceFirst(String.format("%s$", contraction.getKey()), contraction.getValue()))
					// there can be only one (or none, in which case stick with the original value)...
					.findFirst().orElse(street);
		}

		// normalise postcode
		final StringBuilder postcodeBuilder = new StringBuilder(10);
		final String importedPostcode = excelOrder.getPostcode();
		if (importedPostcode != null) {
			postcodeBuilder.append(importedPostcode.replaceAll("\\s+", ""));
			postcodeBuilder.insert(postcodeBuilder.length() > 3 ? postcodeBuilder.length() - 3 : 0, " ");
		}

		final Address address = Address.builder().houseNameNumber(excelOrder.getHouseNameNumber()).street(street).town(excelOrder.getTown())
				.postcode(postcodeBuilder.toString()).build();

		// if there's something in the Address, add the City for Geolocation and return
		if (address.isGeolocatable()) {
			address.setCity(StringUtils.defaultIfEmpty(excelOrder.getCity(), importConfiguration.getDefaultCity()));

			// store Address (if new) for later re-use
			IMPORTED_ADDRESS_CACHE.putIfAbsent(address, address);
			return IMPORTED_ADDRESS_CACHE.get(address);
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
