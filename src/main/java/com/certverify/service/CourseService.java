package com.certverify.service;

import com.certverify.dto.CourseRequest;
import com.certverify.dto.CourseResponse;
import com.certverify.entity.Course;
import com.certverify.exception.ResourceNotFoundException;
import com.certverify.repository.CourseRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        Course course = Course.builder()
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .build();

        Course saved = courseRepository.save(course);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return mapToResponse(course);
    }

    public CourseResponse mapToResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .eventDate(course.getEventDate())
                .build();
    }
}
