package uk.co.gmescouts.stmarys.beddingplants.sales.service;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import uk.co.gmescouts.stmarys.beddingplants.data.OrderRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.PlantRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.SaleRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.CustomerSummary;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.SaleSummary;

@Service
public class SalesService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SalesService.class);

	@Resource
	private SaleRepository saleRepository;

	@Resource
	private PlantRepository plantRepository;

	public Sale saveSale(final Sale sale) {
		LOGGER.info("Saving Sale [{}]", sale.getYear());

		return saleRepository.save(sale);
	}

	public Boolean deleteSale(@NotNull final Integer year) {
		LOGGER.info("Deleting Sale [{}]", year);

		// first check if there is a matching Sale
		final Sale sale = saleRepository.findByYear(year);

		// delete it
		boolean deleted = false;
		if (sale != null) {
			saleRepository.delete(sale);
			deleted = true;
		}

		return deleted;
	}

	public Sale findSaleByYear(@NotNull final Integer year) {
		LOGGER.info("Finding Sale by Year [{}]", year);

		return saleRepository.findByYear(year);
	}

	public Set<Sale> findAllSales() {
		LOGGER.info("Finding all Sales");

		// find and sort by Year
		return new HashSet<>(saleRepository.findAll(Sort.by(Sort.Order.asc("year"))));
	}

	public Plant findPlantByNumAndSaleYear(@NotNull final Integer plantNumber, @NotNull final Integer saleYear) {
		LOGGER.info("Finding Plant [{}] for Sale [{}]", plantNumber, saleYear);

		return plantRepository.findByNumAndSaleYear(plantNumber, saleYear);
	}

	public Boolean deleteSalePlant(@NotNull final Integer plantNumber, @NotNull final Integer year) {
		LOGGER.info("Deleting Plant [{}] from Sale [{}]", plantNumber, year);

		// first check if there is a matching Order
		final Plant plant = plantRepository.findByNumAndSaleYear(plantNumber, year);

		// delete it
		boolean deleted = false;
		if (plant != null) {
			plantRepository.delete(plant);
			deleted = true;
		}

		return deleted;
	}

	public SaleSummary summariseSale(@NotNull final Sale sale) {
		// count Plants and index by Num for easy access later
		final int plantCount = sale.getPlants().size();

		// count Customers and calculate details about their Orders
		final int customerCount = sale.getCustomers().size();
		final int orderCount = sale.getCustomers().stream().map(Customer::getOrders).mapToInt(Set::size).sum();
		// rounded to 2 d.p.
		final double orderCostTotal = Math.round(
				sale.getCustomers().stream().map(Customer::getOrders).mapToDouble(SalesService::calculateOrdersCostTotal).sum() * 100.0) / 100.0;
		// rounded to 2 d.p.
		final double orderIncomeTotal = Math.round(
				sale.getCustomers().stream().map(Customer::getOrders).mapToDouble(SalesService::calculateOrdersIncomeTotal).sum() * 100.0) / 100.0;

		return SaleSummary.builder().year(sale.getYear()).vat(sale.getVat()).plantCount(plantCount).customerCount(customerCount)
				.orderCount(orderCount).orderCostTotal(orderCostTotal).orderIncomeTotal(orderIncomeTotal).build();
	}

	public CustomerSummary summariseCustomer(@NotNull final Customer customer) {
		final int orderCount = customer.getOrders().size();
		// rounded to 2 d.p.
		final double ordersCostTotal = Math.round(calculateOrdersCostTotal(customer.getOrders()) * 100.0) / 100.0;
		// rounded to 2 d.p.
		final double ordersIncomeTotal = Math.round(calculateOrdersIncomeTotal(customer.getOrders()) * 100.0) / 100.0;

		return CustomerSummary.builder().orderCount(orderCount).ordersCostTotal(ordersCostTotal).ordersIncomeTotal(ordersIncomeTotal).build();
	}

	private static Double calculateOrdersCostTotal(@NotNull final Set<Order> orders) {
		return orders.stream().mapToDouble(Order::getCost).sum();
	}

	private static Double calculateOrdersIncomeTotal(@NotNull final Set<Order> orders) {
		return orders.stream().mapToDouble(Order::getPrice).sum();
	}
}
