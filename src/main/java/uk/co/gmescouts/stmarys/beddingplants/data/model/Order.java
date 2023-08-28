package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements PlantSummary {
    @JsonIgnore
    @Id
    public Long getId() {
        // key on the Sale year and Order num
        return Long.valueOf(String.format("%04d%03d", customer.getSale().getSaleYear(), this.num));
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
     * @param plant {@link Plant} to be counted
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

    @Override
    @JsonIgnore
    @Transient
    public Double getPrice() {
        // sum of all OrderItem prices + any Delivery Charge
        return orderItems.stream().mapToDouble(OrderItem::getPrice).sum() + (type == OrderType.DELIVER ? customer.getSale().getDeliveryCharge() : 0.0);
    }

    /**
     * @return {@link #getPrice()} rounded to 2d.p. (for display)
     */
    @JsonIgnore
    @Transient
    public Double getDisplayPrice() {
        return toCurrencyDisplay(this.getPrice());
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

    private Double discount;

    /**
     * @return {@link #getDiscount()} rounded to 2d.p. (for display)
     */
    @JsonIgnore
    @Transient
    public Double getDisplayDiscount() {
        return toCurrencyDisplay(this.getDiscount());
    }

    private Double paid;

    /**
     * @return {@link #getPaid()} rounded to 2d.p. (for display)
     */
    @JsonIgnore
    @Transient
    public Double getDisplayPaid() {
        return toCurrencyDisplay(this.getPaid());
    }

    /**
     * @return Max of 0.0 or ({@link #getPrice()} - ({@link #getDiscount()} + {@link #getPaid()}))
     */
    @JsonIgnore
    @Transient
    public Double getToPay() {
        return Math.max(this.getPrice() - ((this.getDiscount() == null ? 0 : this.getDiscount()) + (this.getPaid() == null ? 0 : this.getPaid())), 0.0);
    }

    /**
     * @return {@link #getToPay()} rounded to 2d.p. (for display)
     */
    @JsonIgnore
    @Transient
    public Double getDisplayToPay() {
        return toCurrencyDisplay(this.getToPay());
    }

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
    @Access(AccessType.FIELD)
    @OrderBy("plant_num")
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "order", fetch = FetchType.EAGER)
    @ToString.Exclude
    private Set<OrderItem> orderItems = new TreeSet<>();

    public void addOrderItem(final OrderItem orderItem) {
        if (orderItem != null) {
            // link Order to OrderItem
            orderItem.setOrder(this);

            // replace existing OrderItem, if present
            orderItems.add(orderItem);
        }
    }

    private Double toCurrencyDisplay(final Double amount) {
        return amount == null || amount == 0 ? null : Math.round(amount * 100.0) / 100.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equals(num, order.num) && Objects.equals(customer, order.customer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, customer);
    }
}
