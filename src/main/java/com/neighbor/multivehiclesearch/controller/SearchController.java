package com.neighbor.multivehiclesearch.controller;

import com.neighbor.multivehiclesearch.model.BestListings;
import com.neighbor.multivehiclesearch.model.VehicleInfo;
import com.neighbor.multivehiclesearch.service.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class SearchController {

    @Autowired
    private ListingService listingService;

    @PostMapping("")
    public List<BestListings> getListingsInfo(@RequestBody List<VehicleInfo> vehicleInfos) {
        return listingService.getBestListings(vehicleInfos);
    }
}
