package uk.co.gmescouts.stmarys.beddingplants.imports.model.excel;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelRow;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class ExcelPlant {
	@ExcelRow
	private int rowIndex;

	@ExcelCellName("Id")
	private String id;

	@ExcelCellName("Plant Name")
	private String name;

	@ExcelCellName("Variety")
	private String variety;

	@ExcelCellName("No of Plants in tray")
	private String details;

	@ExcelCellName("Price inc VAT")
	private String price;

	@ExcelCellName(value = "Cost ex VAT", mandatory = false)
	private String cost;

	public boolean isValid() {
		return StringUtils.isNotBlank(id);
	}
}
