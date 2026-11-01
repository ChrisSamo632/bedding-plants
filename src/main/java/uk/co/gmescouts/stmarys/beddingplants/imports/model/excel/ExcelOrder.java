package uk.co.gmescouts.stmarys.beddingplants.imports.model.excel;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelRow;
import org.apache.commons.lang3.StringUtils;

public record ExcelOrder(
        @ExcelRow int rowIndex,
        @ExcelCellName("Order no") String orderNumber,
        @ExcelCellName("Forename") String forename,
        @ExcelCellName("Surname") String surname,
        @ExcelCellName(value="", expression="Collect \\(C\\) ?/ ?Deliver \\(D\\)") String collectDeliver,
        @ExcelCellName("Delivery Day") String deliveryDay,
        @ExcelCellName("Delivery Route") String deliveryRoute,
        @ExcelCellName("Preferred Collection Slot") String collectionSlot,
        @ExcelCellName("Collection Hour") String collectionHour,
        @ExcelCellName("c/o") String courtesyOf,
        @ExcelCellName("House No") String houseNameNumber,
        @ExcelCellName("Street") String street,
        @ExcelCellName("Town") String town,
        @ExcelCellName("City") String city,
        @ExcelCellName("Postcode") String postcode,
        @ExcelCellName("Email Address") String emailAddress,
        @ExcelCellName("Telephone") String telephone,
        @ExcelCellName("Survey") String survey,
        @ExcelCellName("Notes") String notes,
        @ExcelCellName("1") String numberPlants1,
        @ExcelCellName("2") String numberPlants2,
        @ExcelCellName("3") String numberPlants3,
        @ExcelCellName("4") String numberPlants4,
        @ExcelCellName("5") String numberPlants5,
        @ExcelCellName("6") String numberPlants6,
        @ExcelCellName("7") String numberPlants7,
        @ExcelCellName("8") String numberPlants8,
        @ExcelCellName("9") String numberPlants9,
        @ExcelCellName("10") String numberPlants10,
        @ExcelCellName("11") String numberPlants11,
        @ExcelCellName("12") String numberPlants12,
        @ExcelCellName("13") String numberPlants13,
        @ExcelCellName("14") String numberPlants14,
        @ExcelCellName("15") String numberPlants15,
        @ExcelCellName("16") String numberPlants16,
        @ExcelCellName("17") String numberPlants17,
        @ExcelCellName("18") String numberPlants18,
        @ExcelCellName("19") String numberPlants19,
        @ExcelCellName("20") String numberPlants20,
        @ExcelCellName("21") String numberPlants21,
        @ExcelCellName("22") String numberPlants22,
        @ExcelCellName("23") String numberPlants23,
        @ExcelCellName("24") String numberPlants24,
        @ExcelCellName("25") String numberPlants25,
        @ExcelCellName("26") String numberPlants26,
        @ExcelCellName("27") String numberPlants27,
        @ExcelCellName("28") String numberPlants28,
        @ExcelCellName("29") String numberPlants29,
        @ExcelCellName("30") String numberPlants30,
        @ExcelCellName("Paid") String paid,
        @ExcelCellName("Discount") String discount,
        @ExcelCellName("Payment Type") String paymentType,
        @ExcelCellName("Payment Notes") String paymentNotes) {
    public boolean isValid() {
        return StringUtils.isNotBlank(orderNumber);
    }
}
