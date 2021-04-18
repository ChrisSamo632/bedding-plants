package uk.co.gmescouts.stmarys.beddingplants.imports.model.excel;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelRow;
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

	@ExcelCellName("Collect (C)/Deliver (D)")
	private String collectDeliver;

	@ExcelCellName("Delivery Day")
	private String deliveryDay;

	@ExcelCellName("Preferred Collection Slot")
	private String collectionSlot;

	@ExcelCellName("c/o")
	private String courtesyOf;

	@ExcelCellName("House No")
	private String houseNameNumber;

	@ExcelCellName("Street")
	private String street;

	@ExcelCellName("Town")
	private String town;

	@ExcelCellName("City")
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

	@ExcelCellName("2")
	private String numberPlants2;

	@ExcelCellName("3")
	private String numberPlants3;

	@ExcelCellName("4")
	private String numberPlants4;

	@ExcelCellName("5")
	private String numberPlants5;

	@ExcelCellName("6")
	private String numberPlants6;

	@ExcelCellName("7")
	private String numberPlants7;

	@ExcelCellName("8")
	private String numberPlants8;

	@ExcelCellName("9")
	private String numberPlants9;

	@ExcelCellName("10")
	private String numberPlants10;

	@ExcelCellName("11")
	private String numberPlants11;

	@ExcelCellName("12")
	private String numberPlants12;

	@ExcelCellName("13")
	private String numberPlants13;

	@ExcelCellName("14")
	private String numberPlants14;

	@ExcelCellName("15")
	private String numberPlants15;

	@ExcelCellName("16")
	private String numberPlants16;

	@ExcelCellName("17")
	private String numberPlants17;

	@ExcelCellName("18")
	private String numberPlants18;

	@ExcelCellName("19")
	private String numberPlants19;

	@ExcelCellName("20")
	private String numberPlants20;

	@ExcelCellName("21")
	private String numberPlants21;

	@ExcelCellName("22")
	private String numberPlants22;

	@ExcelCellName("23")
	private String numberPlants23;

	@ExcelCellName("24")
	private String numberPlants24;

	@ExcelCellName("25")
	private String numberPlants25;

	@ExcelCellName("26")
	private String numberPlants26;

	@ExcelCellName("27")
	private String numberPlants27;

	@ExcelCellName("28")
	private String numberPlants28;

	@ExcelCellName("29")
	private String numberPlants29;

	@ExcelCellName("30")
	private String numberPlants30;

	@ExcelCellName("Paid")
	private String paid;

	@ExcelCellName("Discount")
	private String discount;

	public boolean isValid() {
		return StringUtils.isNotBlank(orderNumber);
	}
}
