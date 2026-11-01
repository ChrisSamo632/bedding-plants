package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "deliveryRoutes")
public class DeliveryRoute implements PlantSummary {
    @JsonIgnore
    @Id
    public Long getId() {
        // key on the Sale year and DeliveryRoute num
        return Long.valueOf(String.format("%04d%03d", sale.getSaleYear(), this.num));
    }

    @SuppressWarnings("EmptyMethod")
    public void setId(final Long id) {
        // intentionally blank, only needed for Hibernate
    }

    @SuppressWarnings("null")
    @Override
    @JsonIgnore
    @Transient
    public Integer getCount() {
        return this.orders.stream().mapToInt(Order::getCount).sum();
    }

    @SuppressWarnings("null")
    @Override
    @JsonIgnore
    @Transient
    public Double getPrice() {
        // sum of all OrderItem prices
        return this.orders.stream().mapToDouble(Order::getPrice).sum();
    }

    @SuppressWarnings("null")
    @Override
    @JsonIgnore
    @Transient
    public Double getCost() {
        // sum of all OrderItem costs
        return this.orders.stream().mapToDouble(Order::getCost).sum();
    }

    @NotNull
    @Min(1)
    private Long num;

    @NotNull
    @Enumerated(EnumType.STRING)
    private DeliveryDay deliveryDay;

    @JsonIgnore
    @Access(AccessType.FIELD)
    @ManyToOne
    private Sale sale;

    @SuppressWarnings("null")
    @OrderBy("num")
    @Access(AccessType.FIELD)
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "deliveryRoute")
    private Set<Order> orders = new TreeSet<>(Comparator.comparingInt(Order::getNum));

    public void addOrder(final Order order) {
        if (order != null) {
            // link DeliveryRoute to Order
            order.setDeliveryRoute(this);

            // replace existing Order, if present
            orders.add(order);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliveryRoute that = (DeliveryRoute) o;
        return Objects.equals(num, that.num) && Objects.equals(sale, that.sale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, sale);
    }

    public DeliveryRoute() {
        // required by JPA and Jackson
    }

    public DeliveryRoute(final Long num, final DeliveryDay deliveryDay) {
        this.num = Objects.requireNonNull(num, "num");
        this.deliveryDay = Objects.requireNonNull(deliveryDay, "deliveryDay");
    }

    public Long getNum() {
        return num;
    }

    public void setNum(final Long num) {
        this.num = Objects.requireNonNull(num, "num");
    }

    public DeliveryDay getDeliveryDay() {
        return deliveryDay;
    }

    public void setDeliveryDay(final DeliveryDay deliveryDay) {
        this.deliveryDay = Objects.requireNonNull(deliveryDay, "deliveryDay");
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(final Sale sale) {
        this.sale = sale;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public void setOrders(final Set<Order> orders) {
        this.orders = orders;
    }

    @Override
    public String toString() {
        return "DeliveryRoute(" + "num=" + num + ", " + "deliveryDay=" + deliveryDay + ", " + "sale=" + sale + ")";
    }
}
