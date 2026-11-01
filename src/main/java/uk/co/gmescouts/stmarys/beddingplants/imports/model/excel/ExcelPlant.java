package uk.co.gmescouts.stmarys.beddingplants.imports.model.excel;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelRow;
import org.apache.commons.lang3.StringUtils;

public record ExcelPlant(
        @ExcelRow int rowIndex,
        @ExcelCellName("Id") String id,
        @ExcelCellName("Plant Name") String name,
        @ExcelCellName("Variety") String variety,
        @ExcelCellName("No of Plants in tray") String details,
        @ExcelCellName("Price inc VAT") String price,
        @ExcelCellName("Cost ex VAT") String cost) {
    public boolean isValid() {
        return StringUtils.isNotBlank(id);
    }
}
