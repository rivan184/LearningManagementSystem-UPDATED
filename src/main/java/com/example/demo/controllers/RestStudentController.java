package com.example.demo.controllers;

import com.example.demo.dto.*;
import com.example.demo.models.Employee;
import com.example.demo.models.Module;
import com.example.demo.models.Progress;
import com.example.demo.models.Segment;
import com.example.demo.repositories.CourseRepository;
import com.example.demo.repositories.EmployeeCourseRepository;
import com.example.demo.repositories.EmployeeRepository;
import com.example.demo.repositories.SegmentRepository;
import com.example.demo.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/student")
public class RestStudentController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private EmployeeCourseService employeeCourseService;
    @Autowired
    private SegmentService segmentService;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private ProgressService progressService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private SegmentRepository segmentRepository;


    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity<List<EmployeeResponseDTO>>
    getStudentInfo(Authentication authentication) {
        String email = authentication.getName();
        Employee employee = employeeService.findEmployeeByEmail(email);
        return ResponseEntity.ok().body(employeeService.getById(employee.getId()));
    }

    // FINISHED COURSE PART //
    @GetMapping("/info/finished-course")
    @ResponseBody
    public ResponseEntity<List<List<FinishedCourseResponseDTO>>>
    getCourseInfo(Authentication authentication) {
        String email = authentication.getName();
        Employee employee = employeeService.findEmployeeByEmail(email);
        return ResponseEntity.status(HttpStatus.OK).body(employeeService.getFinishedCourseData(employee.getId()));
    }

    // ONGOING COURSE PART //
    @GetMapping("/info/ongoing-course")
    @ResponseBody
    public ResponseEntity<List<OnGoingCourseResponseDTO>>
    getOnGoingCourseInfo(Authentication authentication) {
        String email = authentication.getName();
        Employee employee = employeeService.findEmployeeByEmail(email);
        return ResponseEntity.status(HttpStatus.OK).body(employeeService.getOnGoingCourseData(employee.getId()));

    }

    @GetMapping("/info/ongoing-course/progress")
    @ResponseBody
    public ResponseEntity<List<ModuleResponseDTO>>
    getOnGoingCourseProgress(Authentication authentication) {
        String email = authentication.getName();
        Employee employee = employeeService.findEmployeeByEmail(email);
        return ResponseEntity.status(HttpStatus.OK).body(employeeService.getModuleProgress(employee.getId()));
    }

    // COURSE LEARNING PART

//    @GetMapping("/info/ongoing-course/{id}")
//    public ResponseEntity<List<LearningCourseResponseDTO>>
//    getCourseByIdEmployee(@PathVariable(required = true) Integer id){
//        return ResponseEntity.ok(employeeCourseService.getCourseByEmployeeIdDTO(id));
//    }

    @GetMapping("/info/ongoing-course/segment")
    public ResponseEntity<List<Segment>> getSegment(Authentication authentication){
        String email = authentication.getName();
        Employee employee = employeeService.findEmployeeByEmail(email);
        Integer courseId = employeeCourseService.getCourseIdByEmployeeId(employee.getId());
        return ResponseEntity.status(HttpStatus.OK).body(segmentRepository.findSegmentByCourseId(courseId));
    }

    @GetMapping("/info/ongoing-course/segment-{segmentId}/module")
    public ResponseEntity<List<Module>> getModule(@PathVariable("segmentId") Integer segmentId, Authentication authentication){
        String email = authentication.getName();
        Employee employee = employeeService.findEmployeeByEmail(email);
        Integer courseId = employeeCourseService.getCourseIdByEmployeeId(employee.getId());
        List<Integer> segmentIdRange = segmentService.filterSegmentIdRange(courseId);

        if(segmentId > segmentIdRange.get(1) || segmentId < segmentIdRange.get(0)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(moduleService.getModuleBySegmentId(segmentId));
    }

    @PostMapping("/info/ongoing-course/segment-{segmentId}/module-{moduleId}/start")
    public ResponseEntity<?> startLearningModule(@PathVariable("segmentId") Integer segmentId,
                                                 @PathVariable("moduleId") Integer moduleId,
                                                 Authentication authentication) {
        String email = authentication.getName();
        Employee employee = employeeService.findEmployeeByEmail(email);
//        Integer courseId = employeeCourseService.getCourseIdByEmployeeId(employee.getId());
        Progress empProgress = progressService.checkFinishedModule(employee.getId(), moduleId);

        List<Integer> moduleIdRange = moduleService.filterModuleIdRange(segmentId);

        if (empProgress == null) {
            if(moduleId <= moduleIdRange.get(1) && moduleId >= moduleIdRange.get(0)) {
                Module module = new Module();
                Progress prog = new Progress();
                module.setId(moduleId);

                prog.setIsFinished(false);
                prog.setEmployee(employee);
                prog.setModule(module);
                return ResponseEntity.status(HttpStatus.OK).body(progressService.saveProgress(prog));
            }
            else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(moduleService.getModuleById(moduleId));
    }

    @Transactional
    @PutMapping("/info/ongoing-course/segment-{segmentId}/module-{moduleId}/finish")
    public ResponseEntity<?> finishLearningModule(@PathVariable("segmentId") Integer segmentId,
                                                  @PathVariable("moduleId") Integer moduleId,
                                                  Authentication authentication) {
        String email = authentication.getName();
        Employee employee = employeeService.findEmployeeByEmail(email);
        Integer courseId = employeeCourseService.getCourseIdByEmployeeId(employee.getId());

        Progress empProgress = progressService.checkFinishedModule(employee.getId(), moduleId);

        String courseName = employeeRepository.getOnGoingCourse(employee.getId());
        Integer courseModule = employeeRepository.getModulePerOnGoingCourse(courseName);

        if(!empProgress.getIsFinished()) {
            progressService.changeIsfinished(moduleId, employee.getId());
            Integer moduleFinished = employeeRepository.totalModuleFinished(employee.getId());
            if (Objects.equals(moduleFinished, courseModule)) {
                employeeRepository.updateCourseStudent(true,employee.getId(),courseId);
                return ResponseEntity.status(HttpStatus.OK).body("Course Finished");
            }
            return ResponseEntity.status(HttpStatus.OK).body("Module Finished");
        }
        return ResponseEntity.status(HttpStatus.OK).body(moduleService.getModuleById(moduleId));
    }

}
