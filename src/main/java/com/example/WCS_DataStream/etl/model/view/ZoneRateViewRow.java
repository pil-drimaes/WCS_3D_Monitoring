package com.example.WCS_DataStream.etl.model.view;

import java.math.BigDecimal;

public record ZoneRateViewRow(String zoneCd, BigDecimal rate, BigDecimal qty) {}


