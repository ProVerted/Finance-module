package com.provert.payroll.config.controller;

import com.provert.payroll.config.dto.HolidayCalendarRequest;
import com.provert.payroll.config.dto.HolidayCalendarResponse;
import com.provert.payroll.config.service.HolidayCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Holiday Calendar", description = "Holiday dates for automatic day-type resolution")
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayCalendarController {

    private final HolidayCalendarService holidayCalendarService;

    @Operation(summary = "Add a holiday to the calendar")
    @PostMapping
    public ResponseEntity<HolidayCalendarResponse> addHoliday(@Valid @RequestBody HolidayCalendarRequest request) {
        HolidayCalendarResponse response = holidayCalendarService.addHoliday(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get holiday by ID")
    @GetMapping("/{id}")
    public ResponseEntity<HolidayCalendarResponse> getHolidayById(@PathVariable Long id) {
        return ResponseEntity.ok(holidayCalendarService.getHolidayById(id));
    }

    @Operation(summary = "List all registered holidays")
    @GetMapping
    public ResponseEntity<List<HolidayCalendarResponse>> getAllHolidays() {
        return ResponseEntity.ok(holidayCalendarService.getAllHolidays());
    }

    @Operation(summary = "Update holiday entry")
    @PutMapping("/{id}")
    public ResponseEntity<HolidayCalendarResponse> updateHoliday(@PathVariable Long id, @Valid @RequestBody HolidayCalendarRequest request) {
        return ResponseEntity.ok(holidayCalendarService.updateHoliday(id, request));
    }

    @Operation(summary = "Delete holiday entry")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayCalendarService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }
}
