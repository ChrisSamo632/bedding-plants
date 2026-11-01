package uk.co.gmescouts.stmarys.beddingplants.api;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Builds the Excel files imported by the API tests. */
final class TestWorkbooks {
    static final String EXAMPLE_WORKBOOK = "Bedding Plants Test Orders - Example.xlsx";

    private static final String[] ORDER_HEADINGS = {"Order no", "Forename", "Surname", "Collect (C) / Deliver (D)", "Delivery Day", "Delivery Route",
            "Preferred Collection Slot", "Collection Hour", "c/o", "House No", "Street", "Town", "City", "Postcode", "Email Address", "Telephone",
            "Survey", "Notes"};
    private static final String[] PLANT_HEADINGS = {"Id", "Plant Name", "Variety", "No of Plants in tray", "Price inc VAT", "Cost ex VAT"};
    private static final int FIRST_PLANT_COLUMN = ORDER_HEADINGS.length;

    private TestWorkbooks() {
    }

    static byte[] example() throws IOException {
        try (InputStream input = new ClassPathResource(EXAMPLE_WORKBOOK).getInputStream()) {
            return input.readAllBytes();
        }
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        private String plantSheetName = "Plant_Imports";
        private String orderSheetName = "Order_Imports";
        private boolean includeOrderSheet = true;
        private final List<String[]> plants = new ArrayList<>();
        private final List<OrderRow> orders = new ArrayList<>();

        Builder sheetNames(final String plantSheet, final String orderSheet) {
            this.plantSheetName = plantSheet;
            this.orderSheetName = orderSheet;
            return this;
        }

        Builder withoutOrderSheet() {
            this.includeOrderSheet = false;
            return this;
        }

        Builder plant(final int id, final String name, final String price, final String cost) {
            plants.add(new String[]{Integer.toString(id), name, "Mixed", "6", price, cost});
            return this;
        }

        /** Adds {@code count} plants priced at 2.00 (cost 1.00) numbered from 1. */
        Builder plants(final int count) {
            for (int id = 1; id <= count; id++) {
                plant(id, "Plant " + id, "£2.00", "£1.00");
            }
            return this;
        }

        Builder order(final OrderRow order) {
            orders.add(order);
            return this;
        }

        byte[] build() throws IOException {
            try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                final Sheet plantSheet = workbook.createSheet(plantSheetName);
                writeRow(plantSheet.createRow(0), PLANT_HEADINGS);
                for (int i = 0; i < plants.size(); i++) {
                    writeRow(plantSheet.createRow(i + 1), plants.get(i));
                }

                if (includeOrderSheet) {
                    final Sheet orderSheet = workbook.createSheet(orderSheetName);
                    final Row headings = orderSheet.createRow(0);
                    writeRow(headings, ORDER_HEADINGS);
                    for (int plant = 1; plant <= plants.size(); plant++) {
                        headings.createCell(FIRST_PLANT_COLUMN + plant - 1).setCellValue(Integer.toString(plant));
                    }
                    final int paidColumn = FIRST_PLANT_COLUMN + plants.size();
                    headings.createCell(paidColumn).setCellValue("Paid");
                    headings.createCell(paidColumn + 1).setCellValue("Discount");

                    for (int i = 0; i < orders.size(); i++) {
                        final OrderRow order = orders.get(i);
                        final Row row = orderSheet.createRow(i + 1);
                        writeRow(row, order.values);
                        for (int plant = 0; plant < order.plantCounts.length; plant++) {
                            if (order.plantCounts[plant] > 0) {
                                row.createCell(FIRST_PLANT_COLUMN + plant).setCellValue(Integer.toString(order.plantCounts[plant]));
                            }
                        }
                        if (order.paid != null) {
                            row.createCell(paidColumn).setCellValue(order.paid);
                        }
                    }
                }

                workbook.write(output);
                return output.toByteArray();
            }
        }

        private static void writeRow(final Row row, final String[] values) {
            for (int column = 0; column < values.length; column++) {
                if (values[column] != null) {
                    row.createCell(column).setCellValue(values[column]);
                }
            }
        }
    }

    static final class OrderRow {
        private final String[] values = new String[ORDER_HEADINGS.length];
        private int[] plantCounts = new int[0];
        private String paid;

        private OrderRow(final int num, final String forename, final String surname, final char collectOrDeliver) {
            values[0] = Integer.toString(num);
            values[1] = forename;
            values[2] = surname;
            values[3] = Character.toString(collectOrDeliver);
        }

        static OrderRow delivery(final int num, final String forename, final String surname) {
            return new OrderRow(num, forename, surname, 'D');
        }

        static OrderRow collection(final int num, final String forename, final String surname) {
            return new OrderRow(num, forename, surname, 'C');
        }

        OrderRow day(final String deliveryDay) {
            values[4] = deliveryDay;
            return this;
        }

        OrderRow route(final int route) {
            values[5] = Integer.toString(route);
            return this;
        }

        OrderRow address(final String house, final String street, final String town, final String postcode) {
            values[9] = house;
            values[10] = street;
            values[11] = town;
            values[13] = postcode;
            return this;
        }

        OrderRow email(final String email) {
            values[14] = email;
            return this;
        }

        /** Number of each plant ordered, where the first value is for plant 1. */
        OrderRow plants(final int... counts) {
            this.plantCounts = counts;
            return this;
        }

        OrderRow paid(final String amount) {
            this.paid = amount;
            return this;
        }
    }
}
