package uk.co.gmescouts.stmarys.beddingplants.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "sales")
@Data
@Builder
@EqualsAndHashCode(of = "year")
@NoArgsConstructor
@AllArgsConstructor
public class Sale {
	@NonNull
	@NotNull
	@Id
	@Column(unique = true)
	private Integer year;

	@NonNull
	@Min(0)
	@NotNull
	private Double vat;

	@NonNull
	@Builder.Default
	@OrderBy("num")
	@OneToMany(cascade = { CascadeType.ALL }, mappedBy = "sale")
	private Set<DeliveryRoute> deliveryRoutes = new TreeSet<>(Comparator.comparing(DeliveryRoute::getNum));

	@NonNull
	@Builder.Default
	@OrderBy("surname, forename")
	@OneToMany(cascade = { CascadeType.ALL }, mappedBy = "sale")
	private Set<Customer> customers = new TreeSet<>(Comparator.comparing(Customer::getName));

	@NonNull
	@Builder.Default
	@OrderBy("num")
	@OneToMany(cascade = { CascadeType.ALL }, mappedBy = "sale")
	private Set<Plant> plants = new TreeSet<>(Comparator.comparingInt(Plant::getNum));

	public void addCustomer(final Customer customer) {
		if (customer != null) {
			// link Sale to Customer
			customer.setSale(this);

			// replace existing Customer, if present
			customers.add(customer);
		}
	}

	public void addPlant(final Plant plant) {
		if (plant != null) {
			// link Sale to Plant
			plant.setSale(this);

			// replace existing Plant, if present
			plants.add(plant);
		}
	}

	public void addDeliveryRoute(final DeliveryRoute deliveryRoute) {
		if (deliveryRoute != null) {
			// link Sale to DeliveryRoute
			deliveryRoute.setSale(this);

			// replace existing DeliveryRoute, if present
			deliveryRoutes.add(deliveryRoute);
		}
	}
}
