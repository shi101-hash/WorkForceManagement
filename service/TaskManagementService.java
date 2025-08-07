package com.example.workforce.service;

import com.example.workforce.dto.*;

import java.util.List;

public interface TaskManagementService {
    TaskManagementDto findTaskById(Long id);
    List<TaskManagementDto> createTasks(TaskCreateRequest request);
    List<TaskManagementDto> updateTasks(UpdateTaskRequest request);
    String assignByReference(AssignByReferenceRequest request);
    List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request);
    TaskManagementDto updatePriority(Long taskId, com.example.workforce.model.enums.Priority priority);
    List<TaskManagementDto> getByPriority(com.example.workforce.model.enums.Priority priority);
}
