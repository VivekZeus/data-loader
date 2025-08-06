package com.example.vivek.app.util;

public class test {
//    private void processTask(TaskDto taskDto) throws Exception {
//        int count=0;
//        boolean shouldDivideTasks = false;
//        if(!taskDto.isHighPriorityTask() && !CacheUtility.getTaskDivision()) {
//            for (TaskDto task : CacheUtility.taskDtoList) {
//                if ( !task.isHighPriorityTask()) {
//                    count++;
//                    if (count == 2) {
//                        shouldDivideTasks = true;
//                        CacheUtility.setTaskDivision(true);
//                        break;
//                    }
//                }
//            }
//        }
//
//
//        DataLoaderMetaData metaData = metaDataRepository.findByTaskId(taskDto.getTaskId()).orElse(null);
//        if (metaData == null || metaData.getStatus() == TaskStatus.CANCELLED) {
//            return;
//        }
//        metaData.setStatus(TaskStatus.IN_PROGRESS);
//        CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.IN_PROGRESS);
//        metaDataRepository.save(metaData);
//
//        long requests = taskDto.isHighPriorityTask()?20:80;
//        if(shouldDivideTasks || CacheUtility.getTaskDivision())requests=(long)requests/2;
//        if(!shouldDivideTasks && CacheUtility.getTaskDivision())CacheUtility.setTaskDivision(false);
//        long pageNumber = metaData.getLastPageProcessed() + 1;
//        long size = Math.min(maxRecords, taskDto.getRequestedRecords());
//
//        for (long i = 0; i < requests; i++) {
//            if (CacheUtility.getTaskStatus(taskDto.getTaskId()) == TaskStatus.CANCELLED) {
//                metaData.setLastPageProcessed(pageNumber);
//                metaDataRepository.save(metaData);
//                return;
//            }
//
//            HttpRecordRespDto apiResp = recordFetcher.getRecords(size, pageNumber);
//            HttpStatus status =  apiResp.getStatus();
//
//            if (status != HttpStatus.OK) {
//                taskDto.setFirst(false);
//                if (status == HttpStatus.TOO_MANY_REQUESTS) {
//                    System.out.println("requeued task due to 429");
//                    taskProducerService.requeueLowPriorityTask(taskDto, metaData, apiResp.getRetryAfter());
//                } else if (status == HttpStatus.BAD_GATEWAY) {
//                    System.out.println("SERVER IS DOWN SO REQUEUED FOR SOME TIME");
//                    CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.WAITING);
//                    metaData.setStatus(TaskStatus.WAITING);
//                    System.out.println("requeued task due to bad gateway");
//                    taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(10).toMillis());
//                }
//                CacheUtility.removeTaskFromList(taskDto);
//                CacheUtility.taskDtoList.add(taskDto);
//                metaData.setLastPageProcessed(pageNumber);
//                metaDataRepository.save(metaData);
//                return;
//            }
//
//            RecordRespDto resp = apiResp.getRespDto();
//            List<RecordDataDto> data = resp.getRecordList();
//
//            if (data.isEmpty() && taskDto.getRequestedRecords() > 0) {
//                taskDto.setFirst(false);
//                CacheUtility.removeTaskFromList(taskDto);
//                CacheUtility.taskDtoList.add(taskDto);
//                System.out.println("requeued task as data is empty or records recieved are 0");
//                taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(2).toMillis());
//
//                return;
//            }
//
//            List<DataTemp> tempList = new ArrayList<>();
//            for (int j = 0; j < data.size(); j++) {
//                RecordDataDto recordDataDto = data.get(j);
//                tempList.add(new DataTemp(
//                        recordDataDto.getName(),
//                        recordDataDto.getRollNo(),
//                        recordDataDto.getAge(),
//                        taskDto.getUserId(),
//                        (pageNumber * size) + (j + 1),
//                        pageNumber
//                ));
//            }
//
//            temporaryDataRepository.saveAll(tempList);
//            CacheUtility.setRecordsProcessed(taskDto.getTaskId(), metaData.getProcessedRecords() + data.size());
//            metaData.setLastPageProcessed(pageNumber);
//            metaData.setProcessedRecords(metaData.getProcessedRecords() + data.size());
//            metaDataRepository.save(metaData);
//
//            taskDto.setRequestedRecords(taskDto.getRequestedRecords() - data.size());
//            if (taskDto.getRequestedRecords() <= 0) break;
//
//            pageNumber += 1;
//            size = Math.min(maxRecords, taskDto.getRequestedRecords());
//        }
//
//        metaData.setLastPageProcessed(pageNumber);
//
//        if (taskDto.getRequestedRecords() > 0) {
//            taskDto.setFirst(false);
//            CacheUtility.removeTaskFromList(taskDto);
//            CacheUtility.taskDtoList.add(taskDto);
//            System.out.println("requeued task because more records left");
//            taskProducerService.requeueLowPriorityTask(taskDto, metaData, Duration.ofMinutes(10).toMillis());
//        } else {
//            metaData.setStatus(TaskStatus.COLLECTED);
//            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COLLECTED);
//
//            moveToMainTable(metaData.getUserId());
//            CacheUtility.removeTaskFromList(taskDto);
//
//            metaData.setStatus(TaskStatus.COMPLETED);
//            CacheUtility.setTaskStatus(taskDto.getTaskId(), TaskStatus.COMPLETED);
//            metaData.setEndedAt(LocalDateTime.now());
//        }
//        metaDataRepository.save(metaData);
//    }
}
