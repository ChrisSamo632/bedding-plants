package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "customers")
@Data
@Builder
@EqualsAndHashCode(of = { "forename", "surname", "sale" })
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
	@JsonIgnore
	@Id
	public String getId() {
		return String.format("%s-%04d", this.getName().toUpperCase(Locale.ROOT), sale.getSaleYear());
	}

	@SuppressWarnings("EmptyMethod")
	public void setId(final String id) {
		// intentionally blank, for Entity/Jackson construction only
	}

	@JsonIgnore
	@Transient
	public String getName() {
		return String.format("%s %s", forename, surname);
	}

	@NonNull
	@NotNull
	private String forename;

	@NonNull
	@NotNull
	private String surname;

	@Access(AccessType.FIELD)
	// don't delete the Address just because the Customer is being removed (may belong to another Customer)
	@ManyToOne(cascade = { CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE })
	private Address address;

	private String emailAddress;

	private String telephone;

	@JsonIgnore
	@Access(AccessType.FIELD)
	@ManyToOne
	private Sale sale;

	@NonNull
	@Builder.Default
	@Access(AccessType.FIELD)
	@OrderBy("num")
	@OneToMany(cascade = { CascadeType.ALL }, mappedBy = "customer")
	@ToString.Exclude
	private Set<Order> orders = new TreeSet<>(Comparator.comparingInt(Order::getNum));

	public void addOrder(final Order order) {
		if (order != null) {
			// link Customer to Order
			order.setCustomer(this);

			// replace existing Order, if present
			orders.add(order);
		}
	}
}
