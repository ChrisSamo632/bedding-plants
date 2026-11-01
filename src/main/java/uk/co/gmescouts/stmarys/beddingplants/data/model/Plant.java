package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "plants")
public class Plant implements Comparable<Plant> {
    @JsonIgnore
    @Id
    public Long getId() {
        // key on the Sale year and Plant num
        return Long.valueOf(String.format("%d%02d", sale.getSaleYear(), this.num));
    }

    @SuppressWarnings("EmptyMethod")
    public void setId(final Long id) {
        // intentionally blank, only needed for Hibernate
    }

    @JsonIgnore
    @Access(AccessType.FIELD)
    @ManyToOne
    private Sale sale;

    @Min(1)
    @NotNull
    private Integer num;

    @NotNull
    private String name;

    @NotNull
    private String variety;

    private String details;

    @Min(0)
    @NotNull
    private Double price;

    @Min(0)
    @NotNull
    private Double cost;

    @Access(AccessType.FIELD)
    @JsonIgnore
    @OneToMany(mappedBy = "plant")
    private Set<OrderItem> orderItems;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Plant plant = (Plant) o;
        return Objects.equals(sale, plant.sale) && Objects.equals(num, plant.num);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sale, num);
    }

    @Override
    public int compareTo(@org.jetbrains.annotations.NotNull final Plant o) {
        return this.getNum().compareTo(o.getNum());
    }

    public Plant() {
        // required by JPA and Jackson
    }

    public Plant(final Integer num, final String name, final String variety, final Double price, final Double cost) {
        this.num = Objects.requireNonNull(num, "num");
        this.name = Objects.requireNonNull(name, "name");
        this.variety = Objects.requireNonNull(variety, "variety");
        this.price = Objects.requireNonNull(price, "price");
        this.cost = Objects.requireNonNull(cost, "cost");
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(final Sale sale) {
        this.sale = sale;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(final Integer num) {
        this.num = Objects.requireNonNull(num, "num");
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(final String variety) {
        this.variety = Objects.requireNonNull(variety, "variety");
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(final Double price) {
        this.price = Objects.requireNonNull(price, "price");
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(final Double cost) {
        this.cost = Objects.requireNonNull(cost, "cost");
    }

    public Set<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(final Set<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @Override
    public String toString() {
        return "Plant(" + "num=" + num + ", " + "name=" + name + ", " + "variety=" + variety + ", " + "details=" + details + ", " + "price=" + price + ", " + "cost=" + cost + ")";
    }
}
