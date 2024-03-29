package uk.co.gmescouts.stmarys.beddingplants.data.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address implements Comparable<Address> {
    @JsonIgnore
    @Id
    public String getId() {
        return StringUtils.upperCase(this.getGeolocatableAddress());
    }

    @SuppressWarnings("EmptyMethod")
    public void setId(final String id) {
        // intentionally blank, for Entity/Jackson construction only
    }

    @JsonIgnore
    @Transient
    public String getGeolocatableAddress() {
        final StringBuilder geo = new StringBuilder(200);

        // house name/number
        geo.append(StringUtils.defaultIfEmpty(houseNameNumber, ""));

        // street
        if (StringUtils.isNotBlank(street)) {
            if (!geo.isEmpty()) {
                geo.append(" ");
            }
            geo.append(street);
        }

        // town
        if (StringUtils.isNotBlank(town)) {
            if (!geo.isEmpty()) {
                geo.append(", ");
            }
            geo.append(town);
        }

        // city
        if (StringUtils.isNotBlank(city)) {
            if (!geo.isEmpty()) {
                geo.append(", ");
            }
            geo.append(city);
        }

        // postcode
        if (StringUtils.isNotBlank(postcode)) {
            if (!geo.isEmpty()) {
                geo.append(", ");
            }
            geo.append(postcode);
        }

        return geo.toString();
    }

    /**
     * @return true if Address contains a non-blank Postcode <b>or</b> Street <u>and</u> Town; otherwise false
     */
    @JsonIgnore
    @Transient
    public boolean isGeolocatable() {
        return StringUtils.isNotBlank(street) && StringUtils.isNotBlank(town) || StringUtils.isNotBlank(postcode);
    }

    /**
     * @return true if Address contains a Geolocation with a non-blank Formatted Address; otherwise false
     */
    @JsonIgnore
    @Transient
    public boolean isGeolocated() {
        return this.geolocation != null && StringUtils.isNotBlank(this.geolocation.getFormattedAddress());
    }

    private String houseNameNumber;

    private String street;

    private String town;

    private String city;

    private String postcode;

    @NonNull
    @JsonIgnore
    @Builder.Default
    @OrderBy("surname, forename")
    @Access(AccessType.FIELD)
    @OneToMany(cascade = {CascadeType.ALL}, mappedBy = "address")
    @ToString.Exclude
    private Set<Customer> customers = new HashSet<>(); // FIXME: new TreeSet<>(Comparator.comparing(Customer::getForename));

    @Embedded
    private Geolocation geolocation;

    public void addCustomer(final Customer customer) {
        if (customer != null) {
            // link Address to Customer
            customer.setAddress(this);

            // replace existing Customer, if present
            customers.add(customer);
        }
    }

    @Override
    public int compareTo(Address o) {
        return new CompareToBuilder()
                .append(this.getCity(), o.getCity())
                .append(this.getTown(), o.getTown())
                .append(this.getStreet(), o.getStreet())
                .append(this.getHouseNameNumber(), o.getHouseNameNumber())
                .toComparison();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address address = (Address) o;
        return Objects.equals(houseNameNumber, address.houseNameNumber) && Objects.equals(street, address.street) && Objects.equals(town, address.town) && Objects.equals(city, address.city) && Objects.equals(postcode, address.postcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(houseNameNumber, street, town, city, postcode);
    }
}
