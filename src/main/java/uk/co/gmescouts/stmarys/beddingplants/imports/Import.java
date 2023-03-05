package uk.co.gmescouts.stmarys.beddingplants.imports;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.imports.service.ImportService;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.SaleSummary;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Calendar;

@RestController
@RequestMapping("/import")
class Import {
	private static final Logger LOGGER = LoggerFactory.getLogger(Import.class);

	private static final String IMPORT_TYPE_EXCEL = "/excel";
	/*
	 * Sales
	 */
	private static final String IMPORT_SALE = "/sale";
	private static final String IMPORT_SALE_EXCEL = IMPORT_SALE + IMPORT_TYPE_EXCEL;

	/*
	 * Customers
	 */
	private static final String IMPORT_CUSTOMERS = "/customers";
	private static final String IMPORT_CUSTOMERS_EXCEL = IMPORT_CUSTOMERS + IMPORT_TYPE_EXCEL;

	/*
	 * Plants
	 */
	private static final String IMPORT_PLANTS = "/plants";
	private static final String IMPORT_PLANTS_EXCEL = IMPORT_PLANTS + IMPORT_TYPE_EXCEL;

	private static final Integer CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

	private static final String MEDIA_TYPE_XLS = "application/vnd.ms-excel";
	private static final String MEDIA_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	@Resource
	private ImportService importService;

	@Resource
	private SalesService salesService;

	@PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MEDIA_TYPE_XLS, MEDIA_TYPE_XLSX },
			produces = MediaType.APPLICATION_JSON_VALUE, value = IMPORT_SALE_EXCEL)
	public SaleSummary importSaleFromExcel(@RequestBody @RequestPart("file") final MultipartFile file, @RequestParam(defaultValue = "20.0") final double vat,
										   @RequestParam(defaultValue = "2.50") final double deliveryCharge, @RequestParam(required = false) final Integer year,
										   @RequestParam(required = false) final String orderImportsSheetName, @RequestParam(required = false) final String plantImportsSheetName)
			throws IOException {

		Integer saleYear;
		if (year == null) {
			// default to current year if orderYear not specified in request
			saleYear = CURRENT_YEAR;
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Year not specified, defaulting to [{}]", CURRENT_YEAR);
			}
		} else {
			// validate year is sensible (e.g. not in future and not too far in the past)
			if (year > CURRENT_YEAR) {
				throw new IllegalArgumentException("Cannot import information for a future year");
			}

			if (year < CURRENT_YEAR - 5) {
				throw new IllegalArgumentException("Cannot import information more than 5 years old");
			}

			saleYear = year;
		}

		// do the import
		final Sale sale = importService.importSaleFromExcelFile(file, saleYear, vat, deliveryCharge, orderImportsSheetName, plantImportsSheetName);

		// summarise the imported Sale
		return salesService.summariseSale(sale);
	}

	@PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MEDIA_TYPE_XLS, MEDIA_TYPE_XLSX },
			produces = MediaType.APPLICATION_JSON_VALUE, value = IMPORT_CUSTOMERS_EXCEL)
	public SaleSummary importCustomersFromExcel(@RequestBody @RequestPart("file") final MultipartFile file, @RequestParam final Integer saleYear,
			@RequestParam(required = false) final String orderImportsSheetName) throws IOException {
		// do the import
		final Sale sale = importService.importCustomersFromExcel(file, orderImportsSheetName, saleYear);

		// summarise the updated Sale
		return salesService.summariseSale(sale);
	}

	@PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MEDIA_TYPE_XLS, MEDIA_TYPE_XLSX },
			produces = MediaType.APPLICATION_JSON_VALUE, value = IMPORT_PLANTS_EXCEL)
	public SaleSummary importPlantsFromExcel(@RequestBody @RequestPart("file") final MultipartFile file, @RequestParam final Integer saleYear,
			@RequestParam(required = false) final String plantImportsSheetName) throws IOException {
		// do the import
		final Sale sale = importService.importPlantsFromExcel(file, plantImportsSheetName, saleYear);

		// summarise the updated Sale
		return salesService.summariseSale(sale);
	}
}
