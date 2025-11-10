package com.example.WCS_DataStream.etl.model.view;

import java.math.BigDecimal;

public record FloorRateViewRow(String zoneCd, String floor, BigDecimal rate, BigDecimal qty) {}


