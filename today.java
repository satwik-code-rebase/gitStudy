@PostMapping("/podmodel/AllCompletedAndCanceledEpic/list/{requestId}")
public PaginationResponse getMyAllCompletedEpics(@PathVariable Long requestId, @Valid @RequestBody PaginationRequest paginationRequest) {
    logger.info("---getAllEpics---");
    Pageable pagination = getPageableObject(paginationRequest);
    String searchString = paginationRequest.getSearchString();
    Page<Epic> allEpics = null;
    Category epicStatusCompleted = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS_COMPLETED);
    Category epicStatusCanceled = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS_CANCELLED);
    if (searchString == null || searchString.trim().equals("")) {
        if (requestId == 1) {
            allEpics = epicRepository.findAllByIsActiveAndEpicStatus(true, epicStatusCompleted, pagination);
        } else if (requestId == 2) {
            allEpics = epicRepository.findAllByIsActiveAndEpicStatus(true, epicStatusCanceled, pagination);
        }

    } else {
//			allEpics = epicRepository.findAllByInputStringAndIsActiveAndIdIn(searchString,currentEpicIds, pagination);
    }

    List<Epic> allEpicsList = allEpics.getContent();

    logger.info("allEpicsList -- " + allEpicsList.size());

    return new PaginationResponse(allEpicsList, allEpics.getTotalPages());
}

@PostMapping("/podmodel/MyAllEpics/list/{parentCustomerName}/{requestId}")
public PaginationResponse getMyAllEpicsData(@PathVariable String parentCustomerName,@PathVariable Long requestId, @Valid @RequestBody PaginationRequest paginationRequest) {
    logger.info("Id:-" + parentCustomerName);
    Category category = null;
    ArrayList<PODDashboardListItem> list = new ArrayList<PODDashboardListItem>();
    List<AssociateAccess> associateAccessList = null;
    Set<String> parentCustomerNames = null;
    List<String> dbNames = null;
    List<HeadCount> headCountList = null;
    Page<Epic> myAllEpic = null;
    List<Epic> allEpicsList = null;
    List<Long> epicIds=null;
    Pageable pagination = getPageableObject(paginationRequest);
    HashMap<String, PODDashboardListItem> mapPODDashboardListItem = new HashMap<String, PODDashboardListItem>();
    Long reportId = headCountService.getLatestReportId();
    String[] allValidStoryArr = {PODConstants.SPRINT_STORY_READY, PODConstants.SPRINT_STORY_INPROGRESS,
            PODConstants.SPRINT_STORY_COMPLETED, PODConstants.SPRINT_STORY_ACCEPTED};

    String[] allValidEpicStatusArr = {PODConstants.EPIC_STATUS_DRAFT, PODConstants.EPIC_STATUS_READY,
            PODConstants.EPIC_STATUS_INPROGRESS, PODConstants.EPIC_STATUS_COMPLETED};

    String[] allCompletedEpicStatusArr = {PODConstants.EPIC_STATUS_COMPLETED};

    List<Category> allValidStoryStatus = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
            PODConstants.STORY_STATUS, PODConstants.STORY_STATUS, Arrays.asList(allValidStoryArr));

    List<Category> allValidEpicStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
            PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, Arrays.asList(allValidEpicStatusArr));

    List<Category> allCompletedEpicStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
            PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, Arrays.asList(allCompletedEpicStatusArr));

    List<String> genCGrades = GradeMapping.getOriginalGenCGradeList();

    switch (getMyMainRole()) {
        case TSC:
            category = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(
                    AccessGroupConstants.ACCESSGROUP_CATEGORY_GROUP, AccessGroupConstants.ACCESSGROUP_CATEGORY_KEY,
                    ERole.BUHead.name());

            associateAccessList = associateAccessRepo.findByAccessTypeAndAssociateId(category, getMyAssociateId());

            dbNames = new ArrayList<String>();
            for (AssociateAccess associateAccess : associateAccessList) {
                dbNames.add(associateAccess.getObjectName());
            }

            category = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(
                    AccessGroupConstants.ACCESSGROUP_CATEGORY_GROUP, AccessGroupConstants.ACCESSGROUP_CATEGORY_KEY,
                    ERole.SBUHead.name());
            epicIds = epicRepository.getEpicIdsBySBU(dbNames, category, genCGrades, true, allValidStoryStatus,
                    allValidEpicStatuses, reportId);
            break;
        case OPS:
            category = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(
                    AccessGroupConstants.ACCESSGROUP_CATEGORY_GROUP, AccessGroupConstants.ACCESSGROUP_CATEGORY_KEY,
                    ERole.SBUHead.name());

            associateAccessList = associateAccessRepo.findByAccessTypeAndAssociateId(category, getMyAssociateId());

            dbNames = new ArrayList<String>();
            for (AssociateAccess associateAccess : associateAccessList) {
                dbNames.add(associateAccess.getObjectName());
            }

            category = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(
                    AccessGroupConstants.ACCESSGROUP_CATEGORY_GROUP, AccessGroupConstants.ACCESSGROUP_CATEGORY_KEY,
                    ERole.PDL.name());
            epicIds = epicRepository.getAllEpicIdsByPDL(dbNames, category, genCGrades, true, allValidStoryStatus,
                    allValidEpicStatuses, reportId);

            break;
        case SBUHead:
        case BUHead:
        case PDL:
        case EDL:
            logger.info("getMyDashboard:EDL/PDL");
            parentCustomerNames = new HashSet<String>();
            reportId = headCountService.getLatestReportId();

//                headCountList = headCountRepo.findByParentCustomerNameInAndReportId(dbNames, reportId);

//                for (HeadCount headCount : headCountList) {
//                    PODDashboardListItem item = mapPODDashboardListItem.get(headCount.getParentCustomerName());
//                    if (item == null) {
//                        item = new PODDashboardListItem();
//                        item.setPortfolio(headCount.getParentCustomerName());
//                        logger.info("headCount.getParentCustomerName() = " + headCount.getParentCustomerName());
//                        mapPODDashboardListItem.put(headCount.getParentCustomerName(), item);
//                    }
//
//                    parentCustomerNames.add(headCount.getParentCustomerName());
//                }
             epicIds = epicRepository.getMyEpicsByParentCustomer(parentCustomerName, reportId,
                    genCGrades, true, allValidStoryStatus, allValidEpicStatuses);
            logger.info("getMyAllEpicIds:EDL/PDL" + epicIds);

            break;

        default:
            break;
    }
    String searchString = paginationRequest.getSearchString();
    if (requestId == 1) {
        if (searchString == null || searchString.trim().equals("")) {
            myAllEpic = epicRepository.findAllByIsActiveAndIdIn(true, epicIds, pagination);
        } else {
            myAllEpic = epicRepository.findAllByInputStringAndIsActiveAndIdIn(searchString, epicIds, pagination);
        }
    } else if (requestId == 2) {
        Category epicStatus = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS_COMPLETED);
        if (searchString == null || searchString.trim().equals("")) {
            myAllEpic = epicRepository.findAllByIsActiveAndIdInAndEpicStatus(true, epicIds, epicStatus, pagination);
        } else {
            myAllEpic = epicRepository.findAllByInputStringAndIsActiveAndEpicStatus(searchString,epicStatus, pagination);
        }
    }

    allEpicsList = myAllEpic.getContent();

    logger.info("allEpicsList -- " + allEpicsList.size());
    return new PaginationResponse(allEpicsList, myAllEpic.getTotalPages());
}

@PostMapping("/podmodel/MyAllSprints/list/{parentCustomerName}/{requestId}")
public PaginationResponse getMyAllSprintData(@PathVariable String parentCustomerName,@PathVariable Long requestId, @Valid @RequestBody PaginationRequest paginationRequest) {
    logger.info("Id:-" + requestId);
    Category category = null;
    ArrayList<PODDashboardListItem> list = new ArrayList<PODDashboardListItem>();
    List<AssociateAccess> associateAccessList = null;
    Set<String> parentCustomerNames = null;
    List<String> dbNames = null;
    List<HeadCount> headCountList = null;
    Page<Sprint> myAllSprints = null;
    List<Sprint> myAllSprintList = null;
    Pageable pagination = getPageableObject(paginationRequest);
    HashMap<String, PODDashboardListItem> mapPODDashboardListItem = new HashMap<String, PODDashboardListItem>();
    Long reportId = headCountService.getLatestReportId();
    String[] allValidStoryArr = {PODConstants.SPRINT_STORY_READY, PODConstants.SPRINT_STORY_INPROGRESS,
            PODConstants.SPRINT_STORY_COMPLETED, PODConstants.SPRINT_STORY_ACCEPTED};
    String[] allValidSprintStatusArr = {PODConstants.SPRINT_STATUS_DRAFT, PODConstants.SPRINT_STATUS_READY,
            PODConstants.SPRINT_STATUS_INPROGRESS, PODConstants.SPRINT_STATUS_COMPLETED};

    List<Category> allValidStoryStatus = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
            PODConstants.STORY_STATUS, PODConstants.STORY_STATUS, Arrays.asList(allValidStoryArr));

    List<Category> allValidSprintStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
            PODConstants.SPRINT_STATUS, PODConstants.SPRINT_STATUS, Arrays.asList(allValidSprintStatusArr));

    List<String> genCGrades = GradeMapping.getOriginalGenCGradeList();

    switch (getMyMainRole()) {
        case BUHead:
            break;
        case SBUHead:
            break;
        case PDL:
        case EDL:
            logger.info("getMyDashboard:EDL/PDL");
            parentCustomerNames = new HashSet<String>();

            category = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(
                    AccessGroupConstants.ACCESSGROUP_CATEGORY_GROUP, AccessGroupConstants.ACCESSGROUP_CATEGORY_KEY,
                    getMyMainRole().name());

            associateAccessList = associateAccessRepo.findByAccessTypeAndAssociateId(category, getMyAssociateId());

            dbNames = new ArrayList<String>();
            for (AssociateAccess associateAccess : associateAccessList) {
                dbNames.add(associateAccess.getObjectName());
            }

            reportId = headCountService.getLatestReportId();

            headCountList = headCountRepo.findByParentCustomerNameInAndReportId(dbNames, reportId);

            for (HeadCount headCount : headCountList) {
                PODDashboardListItem item = mapPODDashboardListItem.get(headCount.getParentCustomerName());
                if (item == null) {
                    item = new PODDashboardListItem();
                    item.setPortfolio(headCount.getParentCustomerName());
                    logger.info("headCount.getParentCustomerName() = " + headCount.getParentCustomerName());
                    mapPODDashboardListItem.put(headCount.getParentCustomerName(), item);
                }

                parentCustomerNames.add(headCount.getParentCustomerName());

            }

            List<Long> sprintsId = sprintRepository.myAllSprintsIdsByParentCustomer(parentCustomerName,
                    reportId, genCGrades, true, allValidStoryStatus, allValidSprintStatuses);

            String searchString = paginationRequest.getSearchString();
            if (requestId == 1) {
                if (searchString == null || searchString.trim().equals("")) {
                    myAllSprints = sprintRepository.findAllByIdIn(sprintsId, pagination);
                } else {
                    myAllSprints = sprintRepository.findAllByInputStringAndActiveAndIdIn(searchString, sprintsId, pagination);
                }
            } else if (requestId == 2) {
                Category sprintStatus = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(PODConstants.SPRINT_STATUS, PODConstants.SPRINT_STATUS, PODConstants.SPRINT_STATUS_COMPLETED);
                if (searchString == null || searchString.trim().equals("")) {
                    myAllSprints = sprintRepository.findAllByIdInAndSprintStatus(sprintsId, sprintStatus, pagination);
                } else {
                    myAllSprints = sprintRepository.findAllByInputStringAndActiveAndIdInAndSprintStatus(searchString,sprintsId,sprintStatus, pagination);
                }
            }

            myAllSprintList = myAllSprints.getContent();

            logger.info("myAllSprintList -- " + myAllSprintList.size());

            break;

        default:
            break;
    }
    return new PaginationResponse(myAllSprintList, myAllSprints.getTotalPages());
}

@PostMapping("/podmodel/MyAllStory/list/{parentCustomerName}/{requestId}")
public PaginationResponse getMyAllStoryData(@PathVariable String parentCustomerName,@PathVariable Long requestId, @Valid @RequestBody PaginationRequest paginationRequest) {
    logger.info("Id:-" + requestId);
    Category category = null;
    ArrayList<PODDashboardListItem> list = new ArrayList<PODDashboardListItem>();
    List<AssociateAccess> associateAccessList = null;
    Set<String> parentCustomerNames = null;
    List<String> dbNames = null;
    List<HeadCount> headCountList = null;
    Page<Story> myAllStory = null;
    List<Story> myAllStoryList = null;
    Pageable pagination = getPageableObject(paginationRequest);
    HashMap<String, PODDashboardListItem> mapPODDashboardListItem = new HashMap<String, PODDashboardListItem>();
    Long reportId = headCountService.getLatestReportId();
    String[] allValidStoryArr = {PODConstants.SPRINT_STORY_READY, PODConstants.SPRINT_STORY_INPROGRESS,
            PODConstants.SPRINT_STORY_COMPLETED, PODConstants.SPRINT_STORY_ACCEPTED};
    String[] allValidSprintStatusArr = {PODConstants.SPRINT_STATUS_DRAFT, PODConstants.SPRINT_STATUS_READY,
            PODConstants.SPRINT_STATUS_INPROGRESS, PODConstants.SPRINT_STATUS_COMPLETED};
    String[] allValidEpicStatusArr = {PODConstants.EPIC_STATUS_DRAFT, PODConstants.EPIC_STATUS_READY,
            PODConstants.EPIC_STATUS_INPROGRESS, PODConstants.EPIC_STATUS_COMPLETED};

    List<Category> allValidStoryStatus = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
            PODConstants.STORY_STATUS, PODConstants.STORY_STATUS, Arrays.asList(allValidStoryArr));

    List<Category> allValidEpicStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
            PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, Arrays.asList(allValidEpicStatusArr));


    List<Category> allValidSprintStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
            PODConstants.SPRINT_STATUS, PODConstants.SPRINT_STATUS, Arrays.asList(allValidSprintStatusArr));

    List<String> genCGrades = GradeMapping.getOriginalGenCGradeList();

    switch (getMyMainRole()) {
        case BUHead:
            break;
        case SBUHead:
            break;
        case PDL:
        case EDL:
            logger.info("getMyDashboard:EDL/PDL");
            parentCustomerNames = new HashSet<String>();

            category = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(
                    AccessGroupConstants.ACCESSGROUP_CATEGORY_GROUP, AccessGroupConstants.ACCESSGROUP_CATEGORY_KEY,
                    getMyMainRole().name());

            associateAccessList = associateAccessRepo.findByAccessTypeAndAssociateId(category, getMyAssociateId());

            dbNames = new ArrayList<String>();
            for (AssociateAccess associateAccess : associateAccessList) {
                dbNames.add(associateAccess.getObjectName());
            }

            reportId = headCountService.getLatestReportId();

            headCountList = headCountRepo.findByParentCustomerNameInAndReportId(dbNames, reportId);

            for (HeadCount headCount : headCountList) {
                PODDashboardListItem item = mapPODDashboardListItem.get(headCount.getParentCustomerName());
                if (item == null) {
                    item = new PODDashboardListItem();
                    item.setPortfolio(headCount.getParentCustomerName());
                    logger.info("headCount.getParentCustomerName() = " + headCount.getParentCustomerName());
                    mapPODDashboardListItem.put(headCount.getParentCustomerName(), item);
                }

                parentCustomerNames.add(headCount.getParentCustomerName());

            }

            List<Long> storyIds = storyRepository.getMyTotalStoriesByParentCustomer(parentCustomerName,
                    reportId, genCGrades, true, allValidStoryStatus);


            String searchString = paginationRequest.getSearchString();
            if (requestId == 1) {
                if (searchString == null || searchString.trim().equals("")) {
                    myAllStory = storyRepository.findAllByIdIn(storyIds, pagination);
                } else {
                    myAllStory = storyRepository.findAllByInputStringAndActiveAndIdIn(searchString, storyIds, pagination);
                }
            } else if (requestId == 2) {
                Category storyStatus = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(PODConstants.STORY_STATUS, PODConstants.STORY_STATUS, PODConstants.SPRINT_STORY_COMPLETED);
                if (searchString == null || searchString.trim().equals("")) {
                    myAllStory = storyRepository.findAllByIdInAndStoryStatus(storyIds, storyStatus, pagination);
                } else {
                    myAllStory = storyRepository.findAllByInputStringAndActiveAndIdInAndStoryStatus(searchString,storyIds,storyStatus, pagination);
                }
            }

            myAllStoryList = myAllStory.getContent();

            logger.info("myAllStoryList -- " + myAllStoryList.size());

            break;

        default:
            break;
    }
    return new PaginationResponse(myAllStoryList, myAllStory.getTotalPages());
}
