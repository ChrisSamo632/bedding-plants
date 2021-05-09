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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "orders")
@Data
@Builder
@EqualsAndHashCode(of = { "customer", "num" })
@ToString(exclude = { "customer" })
@NoArgsConstructor
@AllArgsConstructor
public class Order implements PlantSummary {
	@JsonIgnore
	@Id
	public Long getId() {
		// key on the Sale year and Order num
		return Long.valueOf(String.format("%04d%03d", customer.getSale().getYear(), this.num));
	}

	@SuppressWarnings("EmptyMethod")
	public void setId(final Long id) {
		// intentionally blank, only needed for Hibernate
	}

	@Override
	@JsonIgnore
	@Transient
	public Integer getCount() {
		return this.orderItems.stream().mapToInt(OrderItem::getCount).sum();
	}

	/**
	 * @param plant
	 *            {@link Plant} to be counted
	 * @return number of {@link Plant}s in {@link OrderItem} that matches the input plant
	 */
	@JsonIgnore
	@Transient
	public Integer getPlantCount(@NotNull final Plant plant) {
		final Optional<OrderItem> orderItem = this.orderItems.stream().filter(oi -> oi.getPlant().equals(plant)).findFirst();

		Integer amount = null;
		if (orderItem.isPresent() && orderItem.get().getCount() > 0) {
			amount = orderItem.get().getCount();
		}
		return amount;
	}

	/**
	 * @return {@link #getPrice()} rounded to 2d.p. (for display)
	 */
	@JsonIgnore
	@Transient
	public Double getDisplayPrice() {
		return Math.round(this.getPrice() * 100.0) / 100.0;
	}

	@Override
	@JsonIgnore
	@Transient
	public Double getPrice() {
		// sum of all OrderItem prices
		return orderItems.stream().mapToDouble(OrderItem::getPrice).sum();
	}

	@Override
	@JsonIgnore
	@Transient
	public Double getCost() {
		// sum of all OrderItem costs
		return orderItems.stream().mapToDouble(OrderItem::getCost).sum();
	}

	@NonNull
	@NotNull
	@Min(1)
	private Integer num;

	@JsonIgnore
	@Access(AccessType.FIELD)
	@ManyToOne
	private Customer customer;

	@NonNull
	@NotNull
	@Enumerated(EnumType.STRING)
	private DeliveryDay deliveryDay;

	@Enumerated(EnumType.STRING)
	private CollectionSlot collectionSlot;

	private Integer collectionHour;

	@NonNull
	@NotNull
	@Enumerated(EnumType.STRING)
	private OrderType type;

	private String courtesyOfName;

	private String notes;

	private Float paid;

	private Float discount;

	@JsonIgnore
	@Transient
	public String getCollectionHourFormatted() {
		return collectionHour != null ? String.format("%02d:00", collectionHour) : null;
	}

	@JsonIgnore
	@Access(AccessType.FIELD)
	@ManyToOne
	private DeliveryRoute deliveryRoute;

	@NonNull
	@Builder.Default
	@OrderBy("plant")
	@Access(AccessType.FIELD)
	@OneToMany(cascade = { CascadeType.ALL }, mappedBy = "order")
	private Set<OrderItem> orderItems = new TreeSet<>(Comparator.comparingInt(oi -> oi.getPlant().getNum()));

	public void addOrderItem(final OrderItem orderItem) {
		if (orderItem != null) {
			// link Order to OrderItem
			orderItem.setOrder(this);

			// replace existing OrderItem, if present
			orderItems.add(orderItem);
		}
	}
}
