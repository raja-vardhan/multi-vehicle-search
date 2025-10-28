package com.neighbor.multivehiclesearch.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighbor.multivehiclesearch.model.BestListings;
import com.neighbor.multivehiclesearch.model.Listing;
import com.neighbor.multivehiclesearch.model.VehicleInfo;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Combinations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;


@Service
@Getter
@Slf4j
public class ListingService
{

    public static final String LISTINGS_FILE = "static/listings.json";

    @Getter
    private Map<String, List<Listing>> listingsByLocation = new HashMap<>();
    private List<Listing> allListings = new ArrayList<>();

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws IOException {
        try (InputStream in = new ClassPathResource(LISTINGS_FILE).getInputStream()) {
            allListings = objectMapper.readValue(in, new TypeReference<List<Listing>>() {});
            listingsByLocation = allListings.stream().collect(Collectors.groupingBy(Listing::getLocationId));
        }
    }

    public List<BestListings> getBestListings(List<VehicleInfo> vehicleInfos) {

        List<Integer> vehicleLengths = expandAllVehicleLengths(vehicleInfos);
        if(vehicleLengths.isEmpty()) {
            return Collections.emptyList();
        }

        int totalVehicles = vehicleLengths.size();
        int maxVehicleLen = vehicleLengths.stream().max(Integer::compareTo).orElse(0);

        List<BestListings> result = new ArrayList<>();

        for(Map.Entry<String, List<Listing>> entry : listingsByLocation.entrySet()) {
            String locationId = entry.getKey();
            List<Listing> listings = entry.getValue();

            Optional<Selection> best = findCheapestSelection(listings, vehicleLengths, totalVehicles, maxVehicleLen);
            best.ifPresent(selection -> {
                BestListings bestListingsForLocation = BestListings.builder()
                        .locationId(locationId)
                        .listingIds(selection.listingIds)
                        .totalPriceInCents(selection.totalPriceInCents)
                        .build();
                result.add(bestListingsForLocation);
            });
        }

        result.sort(Comparator.comparingInt(BestListings::getTotalPriceInCents));

        log.info("Request: {}, Count of locations: {}", vehicleInfos, result.size());

        return result;
    }

    private Optional<Selection> findCheapestSelection(List<Listing> listings, List<Integer> vehicleLengths,
            int totalVehicles, int maxVehicleLen) {

        int maxSubsetSize = Math.min(totalVehicles, listings.size());
        List<Listing> sortedListings = new ArrayList<>(listings);
        sortedListings.sort(Comparator.comparingInt(Listing::getPriceInCents));

        Selection best = null;

        for (int k = 1; k <= maxSubsetSize; k++) {
            Combinations combs = new Combinations(sortedListings.size(), k);

            for (int[] idxs : combs) {
                List<Listing> subset = new ArrayList<>(k);
                int priceSum = 0;
                for (int idx : idxs) {
                    Listing L = sortedListings.get(idx);
                    subset.add(L);
                    priceSum += L.getPriceInCents();
                }

                int maxDim = subset.stream()
                        .mapToInt(l -> Math.max(l.getLength(), l.getWidth()))
                        .max().orElse(0);
                if (maxDim < maxVehicleLen) continue;

                int orientations = 1 << k; // 2^k possible rotation assignments
                for (int mask = 0; mask < orientations; mask++) {
                    List<Integer> lanes = new ArrayList<>();
                    for (int i = 0; i < k; i++) {
                        Listing L = subset.get(i);
                        boolean rotated = ((mask >> i) & 1) == 1;

                        int laneLen = rotated ? L.getWidth() : L.getLength();
                        int laneCnt = rotated ? L.getLength() : L.getWidth();

                        if (laneLen < maxVehicleLen) {
                            continue;
                        }
                        for (int c = 0; c < laneCnt; c++) {
                            lanes.add(laneLen);
                        }
                    }

                    if (lanes.size() < totalVehicles){
                        continue;
                    }
                    if (canPack(vehicleLengths, lanes)) {
                        if (best == null || priceSum < best.totalPriceInCents) {
                            best = new Selection(
                                    subset.stream().map(Listing::getId).collect(Collectors.toList()),
                                    priceSum
                            );
                        }
                    }
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private boolean canPack(List<Integer> vehicleLengths, List<Integer> laneCapacities) {
        List<Integer> vehicles = new ArrayList<>(vehicleLengths);
        vehicles.sort(Comparator.reverseOrder());

        TreeMap<Integer, Integer> lanes = new TreeMap<>();
        for (int cap : laneCapacities) {
            lanes.merge(cap, 1, Integer::sum);
        }

        for (int v : vehicles) {
            Map.Entry<Integer, Integer> entry = lanes.ceilingEntry(v);
            if (entry == null){
                return false;
            }

            int remaining = entry.getKey();
            int count = entry.getValue();

            if (count == 1){
                lanes.remove(remaining);
            }
            else{
                lanes.put(remaining, count - 1);
            }

            int newRemaining = remaining - v;
            if (newRemaining > 0){
                lanes.merge(newRemaining, 1, Integer::sum);
            }
        }
        return true;
    }

    private List<Integer> expandAllVehicleLengths(List<VehicleInfo> vehicleInfos) {
        List<Integer> vehicleLengths = new ArrayList<>();
        for(VehicleInfo v : vehicleInfos) {
            int len = v.getLength();
            int quantity = v.getQuantity();
            for(int i = 0; i < quantity; i++) {
                vehicleLengths.add(len);
            }
        }
        return vehicleLengths;
    }

    private static class Selection {
        final List<String> listingIds;
        final int totalPriceInCents;

        Selection(List<String> listingIds, int totalPriceInCents) {
            this.listingIds = listingIds;
            this.totalPriceInCents = totalPriceInCents;
        }
    }
}
