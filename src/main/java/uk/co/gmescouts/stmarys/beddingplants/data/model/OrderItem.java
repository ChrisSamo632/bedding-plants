package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

@Entity
@Table(name = "orderitems")
public class OrderItem implements PlantSummary, Comparable<OrderItem> {
    @JsonIgnore
    @Id
    public Long getId() {
        // key on the Sale year, Order num and Plant num
        return Long.valueOf(String.format("%d%03d%02d", order.getCustomer().getSale().getSaleYear(), order.getNum(), plant.getNum()));
    }

    @SuppressWarnings("EmptyMethod")
    public void setId(final Long id) {
        // intentionally blank, only needed for Hibernate
    }

    @Override
    @JsonIgnore
    @Transient
    public Double getPrice() {
        // plant price inc. VAT * number of plants ordered
        return this.plant.getPrice() * this.count;
    }

    @Override
    @JsonIgnore
    @Transient
    public Double getCost() {
        // (plant cost exc. VAT + VAT) * number of plants ordered
        final double vatMultiplier = 1.0 + (this.plant.getSale().getVat() / 100.0);

        return this.plant.getCost() * vatMultiplier * this.count;
    }

    @JsonIgnore
    @Access(AccessType.FIELD)
    @ManyToOne
    private Order order;

    @Access(AccessType.FIELD)
    @ManyToOne
    private Plant plant;

    @Min(1)
    @NotNull
    private Integer count;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(order, orderItem.order) && Objects.equals(plant, orderItem.plant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, plant);
    }

    @Override
    public int compareTo(@org.jetbrains.annotations.NotNull final OrderItem o) {
        return this.getPlant().getNum().compareTo(o.getPlant().getNum());
    }

    public OrderItem() {
        // required by JPA and Jackson
    }

    public OrderItem(final Plant plant, final Integer count) {
        this.plant = Objects.requireNonNull(plant, "plant");
        this.count = Objects.requireNonNull(count, "count");
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(final Order order) {
        this.order = order;
    }

    public Plant getPlant() {
        return plant;
    }

    public void setPlant(final Plant plant) {
        this.plant = Objects.requireNonNull(plant, "plant");
    }

    @Override
    public Integer getCount() {
        return count;
    }

    public void setCount(final Integer count) {
        this.count = Objects.requireNonNull(count, "count");
    }

    @Override
    public String toString() {
        return "OrderItem(" + "plant=" + plant + ", " + "count=" + count + ")";
    }
}
