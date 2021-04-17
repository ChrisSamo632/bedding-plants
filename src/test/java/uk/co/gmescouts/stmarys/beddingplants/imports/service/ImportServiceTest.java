package uk.co.gmescouts.stmarys.beddingplants.imports.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportServiceTest {
	private final ImportService feature = new ImportService();

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
}
