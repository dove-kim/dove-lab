package com.dove.krx;

import java.time.LocalDate;

public record StockListing(String ticker, String isin, String name, LocalDate listingDate) {}
