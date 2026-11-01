package uk.co.gmescouts.stmarys.beddingplants.sales.model;

import java.util.Objects;

public record CustomerSummary(Integer orderCount, Double ordersCostTotal, Double ordersIncomeTotal) {
	public CustomerSummary {
		Objects.requireNonNull(orderCount, "orderCount");
		Objects.requireNonNull(ordersCostTotal, "ordersCostTotal");
		Objects.requireNonNull(ordersIncomeTotal, "ordersIncomeTotal");
	}
}
