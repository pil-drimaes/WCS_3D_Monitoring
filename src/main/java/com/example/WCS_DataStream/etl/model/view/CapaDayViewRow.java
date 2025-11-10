package com.example.WCS_DataStream.etl.model.view;

import java.math.BigDecimal;

public record CapaDayViewRow(String time, BigDecimal qty, String type) {}


