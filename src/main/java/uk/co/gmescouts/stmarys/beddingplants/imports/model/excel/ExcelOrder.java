package uk.co.gmescouts.stmarys.beddingplants.imports.model.excel;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelRow;
import com.poiji.annotation.ExcelUnknownCells;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class ExcelOrder {
	@ExcelRow
	private int rowIndex;

	@ExcelCellName("Order no")
	private String orderNumber;

	@ExcelCellName("Forename")
	private String forename;

	@ExcelCellName("Surname")
	private String surname;

	@ExcelCellName(value = "Collect (C)/Deliver (D)", mandatory = false)
	private String collectDeliver;

	@ExcelCellName(value = "Delivery Day", mandatory = false)
	private String deliveryDay;

	@ExcelCellName(value = "Delivery Route", mandatory = false)
	private String deliveryRoute;

	@ExcelCellName(value = "Preferred Collection Slot", mandatory = false)
	private String collectionSlot;

	@ExcelCellName(value = "Collection Hour", mandatory = false)
	private String collectionHour;

	@ExcelCellName("c/o")
	private String courtesyOf;

	@ExcelCellName("House No")
	private String houseNameNumber;

	@ExcelCellName("Street")
	private String street;

	@ExcelCellName("Town")
	private String town;

	@ExcelCellName(value = "City", mandatory = false)
	private String city;

	@ExcelCellName("Postcode")
	private String postcode;

	@ExcelCellName("Email Address")
	private String emailAddress;

	@ExcelCellName("Telephone")
	private String telephone;

	@ExcelCellName("Notes")
	private String notes;

	@ExcelCellName("1")
	private String numberPlants1;

	@ExcelCellName(value = "2", mandatory = false)
	private String numberPlants2;

	@ExcelCellName(value = "3", mandatory = false)
	private String numberPlants3;

	@ExcelCellName(value = "4", mandatory = false)
	private String numberPlants4;

	@ExcelCellName(value = "5", mandatory = false)
	private String numberPlants5;

	@ExcelCellName(value = "6", mandatory = false)
	private String numberPlants6;

	@ExcelCellName(value = "7", mandatory = false)
	private String numberPlants7;

	@ExcelCellName(value = "8", mandatory = false)
	private String numberPlants8;

	@ExcelCellName(value = "9", mandatory = false)
	private String numberPlants9;

	@ExcelCellName(value = "10", mandatory = false)
	private String numberPlants10;

	@ExcelCellName(value = "11", mandatory = false)
	private String numberPlants11;

	@ExcelCellName(value = "12", mandatory = false)
	private String numberPlants12;

	@ExcelCellName(value = "13", mandatory = false)
	private String numberPlants13;

	@ExcelCellName(value = "14", mandatory = false)
	private String numberPlants14;

	@ExcelCellName(value = "15", mandatory = false)
	private String numberPlants15;

	@ExcelCellName(value = "16", mandatory = false)
	private String numberPlants16;

	@ExcelCellName(value = "17", mandatory = false)
	private String numberPlants17;

	@ExcelCellName(value = "18", mandatory = false)
	private String numberPlants18;

	@ExcelCellName(value = "19", mandatory = false)
	private String numberPlants19;

	@ExcelCellName(value = "20", mandatory = false)
	private String numberPlants20;

	@ExcelCellName(value = "21", mandatory = false)
	private String numberPlants21;

	@ExcelCellName(value = "22", mandatory = false)
	private String numberPlants22;

	@ExcelCellName(value = "23", mandatory = false)
	private String numberPlants23;

	@ExcelCellName(value = "24", mandatory = false)
	private String numberPlants24;

	@ExcelCellName(value = "25", mandatory = false)
	private String numberPlants25;

	@ExcelCellName(value = "26", mandatory = false)
	private String numberPlants26;

	@ExcelCellName(value = "27", mandatory = false)
	private String numberPlants27;

	@ExcelCellName(value = "28", mandatory = false)
	private String numberPlants28;

	@ExcelCellName(value = "29", mandatory = false)
	private String numberPlants29;

	@ExcelCellName(value = "30", mandatory = false)
	private String numberPlants30;

	@ExcelCellName(value = "Paid", mandatory = false)
	private String paid;

	@ExcelCellName(value = "Discount", mandatory = false)
	private String discount;

	public boolean isValid() {
		return StringUtils.isNotBlank(orderNumber);
	}
}
