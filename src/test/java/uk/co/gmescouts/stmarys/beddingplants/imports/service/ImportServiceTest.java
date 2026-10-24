package uk.co.gmescouts.stmarys.beddingplants.imports.service;

import uk.co.gmescouts.stmarys.beddingplants.data.AddressRepository;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import uk.co.gmescouts.stmarys.beddingplants.imports.configuration.ImportConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportServiceTest {
	private final ImportConfiguration featureConfiguration = new ImportConfiguration();
	private final ImportService feature = new ImportService(featureConfiguration.getPoijiOptionsBuilder(), featureConfiguration, mock(SalesService.class), mock(AddressRepository.class));

	@Test
	final void testNormaliseTelephoneNumber() {
		assertEquals("0161 370 3070", feature.normaliseTelephoneNumber("0161 370 3070"));
		assertEquals("0161 370 3070", feature.normaliseTelephoneNumber("3703070"));
		assertEquals("0161 370 3070", feature.normaliseTelephoneNumber("1613703070"));
		assertEquals("0161 370 3070", feature.normaliseTelephoneNumber(" 370  3070 "));
		assertEquals("0161 370 3070", feature.normaliseTelephoneNumber("01613703070"));

		assertEquals("0786 712 3456", feature.normaliseTelephoneNumber("07867 123 456"));
		assertEquals("0786 712 3456", feature.normaliseTelephoneNumber("7867 123 456"));
		assertEquals("0786 712 3456", feature.normaliseTelephoneNumber("07867123456"));

		assertEquals("07867 12345", feature.normaliseTelephoneNumber("0786712345"));
	}

	@Test
	final void testNormaliseField() {
		assertNull(feature.normaliseField("test", ""));
		assertNull(feature.normaliseField("test", " "));

		assertEquals(" 123 ", feature.normaliseField("test", " 123  "));
		assertEquals(" 1 2 3 ", feature.normaliseField("test", "  1 2  3 "));

		final String fails = "123-456-" + Character.toString(30);
		final IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> feature.normaliseField("test", fails));
		assertEquals("Field [test] value must be ASCII printable: " + fails, iae.getMessage());
	}

	@Test
	void invalidWorkbookDoesNotPersistSale() throws IOException {
		final byte[] workbookContent;
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			workbook.createSheet("Plant_Imports");
			workbook.write(output);
			workbookContent = output.toByteArray();
		}

		final ImportConfiguration configuration = new ImportConfiguration();
		configuration.setOrderImportsName("Order_Imports");
		configuration.setPlantImportsName("Plant_Imports");
		final SalesService salesService = mock(SalesService.class);
		final ImportService service = new ImportService(configuration.getPoijiOptionsBuilder(), configuration, salesService, mock(AddressRepository.class));
		final MockMultipartFile file = new MockMultipartFile("file", "orders.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookContent);

		assertThrows(IllegalStateException.class, () -> service.importSaleFromExcelFile(file, 2026, 20.0, 2.5, null, null));
		verify(salesService, never()).saveSale(any());
	}

	@Test
	void repeatedSaleImportDoesNotDuplicatePlantsCustomersOrOrders() throws IOException {
		final ImportConfiguration configuration = new ImportConfiguration();
		configuration.setOrderImportsName("Order_Imports");
		configuration.setPlantImportsName("Plant_Imports");
		configuration.setDefaultCity("Manchester");
		final SalesService salesService = mock(SalesService.class);
		final AtomicReference<Sale> persistedSale = new AtomicReference<>();
		when(salesService.findSaleByYear(2026)).thenAnswer(invocation -> persistedSale.get());
		when(salesService.saveSale(any())).thenAnswer(invocation -> {
			final Sale sale = invocation.getArgument(0);
			persistedSale.set(sale);
			return sale;
		});
		final ImportService service = new ImportService(configuration.getPoijiOptionsBuilder(), configuration, salesService, mock(AddressRepository.class));
		final MockMultipartFile file = new MockMultipartFile("file", "orders.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", validWorkbook());

		service.importSaleFromExcelFile(file, 2026, 20.0, 2.5, null, null);
		final Sale repeatedSale = service.importSaleFromExcelFile(file, 2026, 20.0, 2.5, null, null);

		assertEquals(1, repeatedSale.getPlants().size());
		assertEquals(1, repeatedSale.getCustomers().size());
		assertEquals(1, repeatedSale.getCustomers().iterator().next().getOrders().size());
		verify(salesService, times(2)).saveSale(any());
	}

	private byte[] validWorkbook() throws IOException {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			final org.apache.poi.ss.usermodel.Sheet plants = workbook.createSheet("Plant_Imports");
			final var plantHeaders = plants.createRow(0);
			final String[] plantHeaderValues = {"Id", "Plant Name", "Variety", "No of Plants in tray", "Price inc VAT", "Cost ex VAT"};
			for (int index = 0; index < plantHeaderValues.length; index++) {
				plantHeaders.createCell(index).setCellValue(plantHeaderValues[index]);
			}
			final var plant = plants.createRow(1);
			plant.createCell(0).setCellValue("1");
			plant.createCell(1).setCellValue("Petunia");
			plant.createCell(2).setCellValue("Mixed");
			plant.createCell(3).setCellValue("12");
			plant.createCell(4).setCellValue("1.00");
			plant.createCell(5).setCellValue("0.50");

			final org.apache.poi.ss.usermodel.Sheet orders = workbook.createSheet("Order_Imports");
			final var orderHeaders = orders.createRow(0);
			final String[] orderHeaderValues = {"Order no", "Forename", "Surname", "Collect (C) / Deliver (D)", "1"};
			for (int index = 0; index < orderHeaderValues.length; index++) {
				orderHeaders.createCell(index).setCellValue(orderHeaderValues[index]);
			}
			final var order = orders.createRow(1);
			order.createCell(0).setCellValue("1");
			order.createCell(1).setCellValue("Test");
			order.createCell(2).setCellValue("Customer");
			order.createCell(3).setCellValue("Collect (C)");
			order.createCell(4).setCellValue("1");
			workbook.write(output);
			return output.toByteArray();
		}
	}
}
