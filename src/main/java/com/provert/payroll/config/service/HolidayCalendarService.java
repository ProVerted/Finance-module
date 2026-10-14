package com.provert.payroll.config.service;

import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.common.exception.ResourceNotFoundException;
import com.provert.payroll.config.dto.HolidayCalendarRequest;
import com.provert.payroll.config.dto.HolidayCalendarResponse;
import com.provert.payroll.config.entity.HolidayCalendar;
import com.provert.payroll.config.repository.HolidayCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HolidayCalendarService {

    private final HolidayCalendarRepository holidayCalendarRepository;

    @Transactional
    public HolidayCalendarResponse addHoliday(HolidayCalendarRequest request) {
        if (holidayCalendarRepository.existsByHolidayDate(request.getHolidayDate())) {
            throw new ConflictException("Holiday for date " + request.getHolidayDate() + " already exists", "HOLIDAY_ALREADY_EXISTS");
        }

        HolidayCalendar holiday = new HolidayCalendar();
        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setDescription(request.getDescription());
        holiday.setActive(true);

        HolidayCalendar saved = holidayCalendarRepository.save(holiday);
        return mapToResponse(saved);
    }

    public HolidayCalendarResponse getHolidayById(Long id) {
        HolidayCalendar holiday = holidayCalendarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        return mapToResponse(holiday);
    }

    public boolean isHoliday(LocalDate date) {
        return holidayCalendarRepository.existsByHolidayDate(date);
    }

    public List<HolidayCalendarResponse> getAllHolidays() {
        return holidayCalendarRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public HolidayCalendarResponse updateHoliday(Long id, HolidayCalendarRequest request) {
        HolidayCalendar holiday = holidayCalendarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));

        if (!holiday.getHolidayDate().equals(request.getHolidayDate()) &&
                holidayCalendarRepository.existsByHolidayDate(request.getHolidayDate())) {
            throw new ConflictException("Holiday already exists for date: " + request.getHolidayDate(), "HOLIDAY_ALREADY_EXISTS");
        }

        holiday.setHolidayDate(request.getHolidayDate());
        holiday.setDescription(request.getDescription());

        HolidayCalendar updated = holidayCalendarRepository.save(holiday);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteHoliday(Long id) {
        HolidayCalendar holiday = holidayCalendarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + id));
        holidayCalendarRepository.delete(holiday);
    }

    private HolidayCalendarResponse mapToResponse(HolidayCalendar holiday) {
        return HolidayCalendarResponse.builder()
                .id(holiday.getId())
                .holidayDate(holiday.getHolidayDate())
                .description(holiday.getDescription())
                .active(holiday.getActive())
                .createdAt(holiday.getCreatedAt())
                .updatedAt(holiday.getUpdatedAt())
                .build();
    }
}
