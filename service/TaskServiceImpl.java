package com.example.workforce.service;

import com.example.workforce.dto.*;
import com.example.workforce.exception.ResourceNotFoundException;
import com.example.workforce.mapper.TaskManagementMapperImpl;
import com.example.workforce.model.TaskManagement;
import com.example.workforce.model.enums.Priority;
import com.example.workforce.model.enums.Task;
import com.example.workforce.model.enums.TaskStatus;
import com.example.workforce.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskManagementService {

    private final TaskRepository taskRepository;
    private final TaskManagementMapperImpl mapper;

    public TaskServiceImpl(TaskRepository taskRepository, TaskManagementMapperImpl mapper) {
        this.taskRepository = taskRepository;
        this.mapper = mapper;
    }

    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return mapper.modelToDto(task);
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest request) {
        List<TaskManagement> created = new ArrayList<>();
        for (TaskCreateRequest.RequestItem item : request.getRequests()) {
            TaskManagement task = new TaskManagement();
            task.setReferenceId(item.getReferenceId());
            task.setReferenceType(item.getReferenceType());
            task.setTask(item.getTask());
            task.setAssigneeId(item.getAssigneeId());
            task.setPriority(item.getPriority());
            task.setTaskDeadlineTime(item.getTaskDeadlineTime());
            task.setStatus(TaskStatus.ASSIGNED);
            task.setDescription("New task created.");
            created.add(taskRepository.save(task));
        }
        return mapper.modelListToDtoList(created);
    }

    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest request) {
        List<TaskManagement> updated = new ArrayList<>();
        for (UpdateTaskRequest.RequestItem item : request.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));
            if (item.getTaskStatus() != null) task.setStatus(item.getTaskStatus());
            if (item.getDescription() != null) task.setDescription(item.getDescription());
            updated.add(taskRepository.save(task));
        }
        return mapper.modelListToDtoList(updated);
    }

    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());

        for (Task taskType : applicableTasks) {
            List<TaskManagement> existingOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .collect(Collectors.toList());

            for (TaskManagement oldTask : existingOfType) {
                oldTask.setStatus(TaskStatus.CANCELLED);
                taskRepository.save(oldTask);
            }

            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(request.getReferenceId());
            newTask.setReferenceType(request.getReferenceType());
            newTask.setTask(taskType);
            newTask.setAssigneeId(request.getAssigneeId());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("Task reassigned");
            taskRepository.save(newTask);
        }

        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }

    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        return taskRepository.findByAssigneeIdIn(request.getAssigneeIds()).stream()
                .filter(task -> task.getStatus() != TaskStatus.CANCELLED &&
                        ((task.getTaskDeadlineTime() >= request.getStartDate() &&
                                task.getTaskDeadlineTime() <= request.getEndDate()) ||
                                (task.getTaskDeadlineTime() < request.getStartDate() &&
                                        task.getStatus() != TaskStatus.COMPLETED)))
                .map(mapper::modelToDto)
                .collect(Collectors.toList());
    }

    @Override
    public TaskManagementDto updatePriority(Long taskId, Priority priority) {
        TaskManagement task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        task.setPriority(priority);
        taskRepository.save(task);
        return mapper.modelToDto(task);
    }

    @Override
    public List<TaskManagementDto> getByPriority(Priority priority) {
        return taskRepository.findAll().stream()
                .filter(task -> task.getPriority() == priority)
                .map(mapper::modelToDto)
                .collect(Collectors.toList());
    }
}
