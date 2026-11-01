package uk.co.gmescouts.stmarys.beddingplants.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import uk.co.gmescouts.stmarys.beddingplants.data.AddressRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.SaleRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Geolocation;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.service.RoadDistanceService;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.service.GeolocationService;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * Boots the whole application (real controllers, services, JPA/H2 and Thymeleaf templates). Only the calls out to Google
 * (geolocation, map images and road distances) are replaced, with deterministic fakes.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "spring.profiles.active=release",
                "spring.datasource.url=jdbc:h2:mem:api-tests;DB_CLOSE_DELAY=-1",
                "spring.datasource.password=",
                "beddingplants.geolocation.google-api-key=test-key"
        })
abstract class AbstractApiTest {
    static {
        // Hibernate asserts that an entity's computed id is readable while its associations are still being hydrated, which the
        // computed-id entities here cannot satisfy; the assertion is skipped in production so skip it for these tests as well
        ClassLoader.getSystemClassLoader().setPackageAssertionStatus("org.hibernate", false);
    }

    static final int YEAR = Year.now().getValue();

    private static final double HUT_LAT = 53.4786893;
    private static final double HUT_LNG = -2.1429603;
    private static final double EARTH_RADIUS_METRES = 6_371_000;

    private static final int PORT = freePort();

    @DynamicPropertySource
    static void serverPort(final DynamicPropertyRegistry registry) {
        // the PDF exports call back into the running server, so it needs a known port
        registry.add("server.port", () -> PORT);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected JsonMapper jsonMapper;

    @Autowired
    protected SaleRepository saleRepository;

    @Autowired
    protected AddressRepository addressRepository;

    @MockitoBean
    protected GeolocationService geolocationService;

    @MockitoBean
    protected RoadDistanceService roadDistanceService;

    protected MockMvc mockMvc;

    /** Fake geocoder results keyed by a fragment of the address (e.g. a postcode). */
    private final Map<String, Geolocation> knownLocations = new ConcurrentHashMap<>();

    @SuppressWarnings("null")
    @BeforeEach
    void setUpApi() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        knownLocations.clear();

        // geocode to a known location if the address contains its key, otherwise somewhere close to the Scout Hut
        when(geolocationService.geolocateGeolocatableAddress(anyString())).thenAnswer(invocation -> {
            final String address = invocation.getArgument(0, String.class);
            return knownLocations.entrySet().stream().filter(entry -> address.toUpperCase().contains(entry.getKey().toUpperCase()))
                    .map(Map.Entry::getValue).findFirst()
                    .orElseGet(() -> new Geolocation("Formatted " + address, HUT_LAT + Math.floorMod(address.hashCode(), 50) / 100_000.0, HUT_LNG));
        });

        // straight-line distances (in metres) stand in for the Google Distance Matrix
        when(roadDistanceService.getRoadDistanceMatrix(anyList())).thenAnswer(invocation -> {
            final List<Geolocation> locations = invocation.getArgument(0);
            final double[][] matrix = new double[locations.size()][locations.size()];
            for (int from = 0; from < matrix.length; from++) {
                for (int to = 0; to < matrix.length; to++) {
                    matrix[from][to] = distanceMetres(locations.get(from), locations.get(to));
                }
            }
            return matrix;
        });
    }

    @AfterEach
    void cleanDatabase() {
        saleRepository.deleteAll();
        addressRepository.deleteAll();
    }

    protected void geolocate(final String addressFragment, final double lat, final double lng) {
        knownLocations.put(addressFragment, new Geolocation("Formatted " + addressFragment, lat, lng));
    }

    protected ResultActions importSale(final byte[] workbook, final int year) throws Exception {
        return mockMvc.perform(multipart("/import/sale/excel").file(workbookFile(workbook)).param("year", Integer.toString(year)));
    }

    protected ResultActions importExampleSale(final int year) throws Exception {
        return importSale(TestWorkbooks.example(), year);
    }

    protected static MockMultipartFile workbookFile(final byte[] workbook) {
        return new MockMultipartFile("file", "orders.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook);
    }

    protected static int serverPort() {
        return PORT;
    }

    private static double distanceMetres(final Geolocation first, final Geolocation second) {
        final double lat1 = Math.toRadians(first.latitude());
        final double lat2 = Math.toRadians(second.latitude());
        final double dLat = lat2 - lat1;
        final double dLng = Math.toRadians(second.longitude() - first.longitude());
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_METRES * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException e) {
            throw new IllegalStateException("Unable to find a free port for the API tests", e);
        }
    }
}
