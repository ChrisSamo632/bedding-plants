package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "customers")
public class Customer {
    @JsonIgnore
    @Id
    public String getId() {
        if (sale == null) {
            throw new IllegalStateException("Customer must be linked to a Sale before its id can be determined");
        }
        return String.format("%s-%04d", this.getName().toUpperCase(Locale.ROOT), sale.getSaleYear());
    }

    @SuppressWarnings({"EmptyMethod"})
    public void setId(final String id) {
        // intentionally blank, for Entity/Jackson construction only
    }

    @JsonIgnore
    @Transient
    public String getName() {
        return String.format("%s %s", forename, surname);
    }

    @NotNull
    private String forename;

    @NotNull
    private String surname;

    @Access(AccessType.FIELD)
    // don't delete the Address just because the Customer is being removed (may belong to another Customer)
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
    private Address address;

    private String emailAddress;

    private String telephone;

    @JsonIgnore
    @Access(AccessType.FIELD)
    @ManyToOne
    private Sale sale;

    @SuppressWarnings("null")
    @Access(AccessType.FIELD)
    @OrderBy("num")
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "customer")
    private Set<Order> orders = new TreeSet<>(Comparator.comparingInt(Order::getNum));

    public void addOrder(final Order order) {
        if (order != null) {
            // link Customer to Order
            order.setCustomer(this);

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
        Customer customer = (Customer) o;
        return Objects.equals(forename, customer.forename) && Objects.equals(surname, customer.surname) && Objects.equals(sale, customer.sale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forename, surname, sale);
    }

    public Customer() {
        // required by JPA and Jackson
    }

    public Customer(final String forename, final String surname) {
        this.forename = Objects.requireNonNull(forename, "forename");
        this.surname = Objects.requireNonNull(surname, "surname");
    }

    public String getForename() {
        return forename;
    }

    public void setForename(final String forename) {
        this.forename = Objects.requireNonNull(forename, "forename");
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(final String surname) {
        this.surname = Objects.requireNonNull(surname, "surname");
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(final String telephone) {
        this.telephone = telephone;
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
        this.orders = Objects.requireNonNull(orders, "orders");
    }

    @Override
    public String toString() {
        return "Customer(" + "forename=" + forename + ", " + "surname=" + surname + ", " + "address=" + address + ", " + "emailAddress=" + emailAddress + ", " + "telephone=" + telephone + ", " + "sale=" + sale + ")";
    }
}
