package com.neighbor.multivehiclesearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BestListings
{

    @JsonProperty("location_id")
    String locationId;

    @JsonProperty("listing_ids")
    List<String> listingIds;

    @JsonProperty("total_price_in_cents")
    int totalPriceInCents;
}
