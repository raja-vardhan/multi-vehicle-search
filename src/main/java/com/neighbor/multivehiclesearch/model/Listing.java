package com.neighbor.multivehiclesearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Listing
{

    String id;

    @JsonProperty("location_id")
    String locationId;

    int length;

    int width;

    @JsonProperty("price_in_cents")
    int priceInCents;
}
