package com.neighbor.multivehiclesearch.service;

import com.neighbor.multivehiclesearch.model.BestListings;
import com.neighbor.multivehiclesearch.model.Listing;
import com.neighbor.multivehiclesearch.model.VehicleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ListingServiceTest
{
    private ListingService service;

    @BeforeEach
    void setUp()
    {
        service = new ListingService();

        Map<String, List<Listing>> listingsByLocation = new HashMap<>();

        // Location A with two listings
        listingsByLocation.put("A", Arrays.asList(
                Listing.builder()
                        .id("L1")
                        .locationId("A")
                        .length(20)
                        .width(20)
                        .priceInCents(5000)
                        .build(),
                Listing.builder()
                        .id("L2")
                        .locationId("A")
                        .length(10)
                        .width(30)
                        .priceInCents(4000)
                        .build()
        ));

        // Location B with one large listing
        listingsByLocation.put("B", List.of(
                Listing.builder()
                        .id("L3").
                        locationId("B")
                        .length(40)
                        .width(10)
                        .priceInCents(3000)
                        .build()
        ));

        service.getListingsByLocation().putAll(listingsByLocation);
    }

    @Test
    void whenRequestIsEmpty_thenReturnEmptyList () {
        List<BestListings> result = service.getBestListings(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    void whenSingleVehicle_fitsSingleListing() {
        VehicleInfo v = VehicleInfo.builder()
                .length(40)
                .quantity(1)
                .build();

        List<BestListings> result = service.getBestListings(List.of(v));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocationId()).isEqualTo("B");
        assertThat(result.get(0).getTotalPriceInCents()).isEqualTo(3000);
    }

    @Test
    void whenSingleVehicle_fitsBothListings() {
        VehicleInfo v = VehicleInfo.builder()
                .length(10)
                .quantity(1)
                .build();

        List<BestListings> result = service.getBestListings(List.of(v));
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLocationId()).isEqualTo("B");
        assertThat(result.get(1).getLocationId()).isEqualTo("A");
        assertThat(result.get(0).getTotalPriceInCents()).isEqualTo(3000);
        assertThat(result.get(1).getTotalPriceInCents()).isEqualTo(4000);
    }
}
