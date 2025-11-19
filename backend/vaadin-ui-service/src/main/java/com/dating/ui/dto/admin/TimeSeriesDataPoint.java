package com.dating.ui.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for time series data points (charts)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataPoint {
    private LocalDate date;
    private long value;
    private String label;
}
