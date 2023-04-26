package com.example.myjwt.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.myjwt.beans.GradeMapping;
import com.example.myjwt.controllers.base.GenCTrackerBaseController;
import com.example.myjwt.models.AssociateAccess;
import com.example.myjwt.models.Category;
import com.example.myjwt.models.Epic;
import com.example.myjwt.models.HeadCount;
import com.example.myjwt.models.Sprint;
import com.example.myjwt.models.Story;
import com.example.myjwt.models.enm.ERole;
import com.example.myjwt.payload.request.EpicRequest;
import com.example.myjwt.payload.request.PaginationRequest;
import com.example.myjwt.payload.request.SprintAddScrumMasterOrTechLeadRequest;
import com.example.myjwt.payload.request.SprintCreateRequest;
import com.example.myjwt.payload.response.ApiResponse;
import com.example.myjwt.payload.response.PODDashboardListItem;
import com.example.myjwt.payload.response.PaginationResponse;
import com.example.myjwt.payload.response.StoryResponse;
import com.example.myjwt.repo.AssociateAccessRepo;
import com.example.myjwt.repo.BillablePlanRepository;
import com.example.myjwt.repo.CategoryRepository;
import com.example.myjwt.repo.EpicRepository;
import com.example.myjwt.repo.HeadCountRepo;
import com.example.myjwt.repo.PODRoleRepo;
import com.example.myjwt.repo.SprintRepository;
import com.example.myjwt.repo.StoryRepository;
import com.example.myjwt.security.services.EpicService;
import com.example.myjwt.security.services.SprintService;
import com.example.myjwt.services.HeadCountService;
import com.example.myjwt.util.AccessGroupConstants;
import com.example.myjwt.util.BillableConstants;
import com.example.myjwt.util.PMUtils;
import com.example.myjwt.util.PODConstants;
import com.example.myjwt.views.BillableGencsWithGradeView;
import com.example.myjwt.views.GencsWithStoriesView;

@Controller
@RequestMapping("/api")
public class PodController extends GenCTrackerBaseController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    EpicService epicService;
    @Autowired
    EpicRepository epicRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SprintService sprintService;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private HeadCountRepo headCountRepo;
    @Autowired
    private HeadCountService headCountService;
    @Autowired
    private AssociateAccessRepo associateAccessRepo;
    @Autowired
    private BillablePlanRepository billablePlanRepository;
    @Autowired
    private PODRoleRepo podRoleRepo;

    @PostMapping("/podmodel/epic/add")
    public ResponseEntity<?> addEpic(@RequestBody EpicRequest epicRequest) {
        epicService.addEpic(epicRequest);
        return ResponseEntity.ok().body(new ApiResponse(true, "Epic created"));
    }

    @PostMapping("/podmodel/epic/list")
    public PaginationResponse getAllEpics(@Valid @RequestBody PaginationRequest paginationRequest) {
        logger.info("---getAllEpics---");

        Pageable pagination = getPageableObject(paginationRequest);

        String searchString = paginationRequest.getSearchString();

        Page<Epic> allEpics = null;

        if (searchString == null || searchString.trim().equals("")) {
            allEpics = epicRepository.findByIsActiveOrderByIdDesc(true, pagination);
        } else {
            allEpics = epicRepository.findAllByInputStringAndActive(searchString, pagination);
        }

        List<Epic> allEpicsList = allEpics.getContent();

        logger.info("allEpicsList -- " + allEpicsList.size());

        return new PaginationResponse(allEpicsList, allEpics.getTotalPages());
    }

    @PostMapping("/podmodel/MyAllEpic/list")
    public PaginationResponse getMyAllEpics(@Valid @RequestBody PaginationRequest paginationRequest) {
        logger.info("---getAllEpics---");

        Pageable pagination = getPageableObject(paginationRequest);
        List<Long> myNominatedTeamData = podRoleRepo.getMyNominatedTeamAssociateIDs(getMyAssociateId());
        logger.info("--" + myNominatedTeamData);
        List<Epic> myCurrentEpic = storyRepository.getMyAllEpicsByDevelopersId(myNominatedTeamData);
        List<Long> currentEpicIds = new ArrayList<>();
        for (Epic epic : myCurrentEpic) {
            currentEpicIds.add(epic.getId());
        }
        logger.info("--" + myCurrentEpic);
        String searchString = paginationRequest.getSearchString();
        Page<Epic> allEpics = null;
//        Category epicStatus=categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(PODConstants.EPIC_STATUS,PODConstants.EPIC_STATUS,PODConstants.EPIC_STATUS_COMPLETED);
        if (searchString == null || searchString.trim().equals("")) {
            allEpics = epicRepository.findAllByIsActiveAndIdIn(true, currentEpicIds, pagination);
        } else {
            allEpics = epicRepository.findAllByInputStringAndIsActiveAndIdIn(searchString, currentEpicIds, pagination);
        }

        List<Epic> allEpicsList = allEpics.getContent();

        logger.info("allEpicsList -- " + allEpicsList.size());

        return new PaginationResponse(allEpicsList, allEpics.getTotalPages());
    }

    @PostMapping("/podmodel/AllCompletedAndCanceledEpic/list/{requestId}")
    public PaginationResponse getMyAllCompletedEpics(@PathVariable Long requestId,@Valid @RequestBody PaginationRequest paginationRequest) {
        logger.info("---getAllEpics---");
        Pageable pagination = getPageableObject(paginationRequest);
        String searchString = paginationRequest.getSearchString();
        Page<Epic> allEpics = null;
        Category epicStatusCompleted = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS_COMPLETED);
        Category epicStatusCanceled = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS_CANCELLED);
        if (searchString == null || searchString.trim().equals("")) {
            if(requestId==1) {
                allEpics = epicRepository.findAllByIsActiveAndEpicStatus(true, epicStatusCompleted, pagination);
            }else if(requestId==2){
                allEpics = epicRepository.findAllByIsActiveAndEpicStatus(true,epicStatusCanceled , pagination);
            }

        } else {
//			allEpics = epicRepository.findAllByInputStringAndIsActiveAndIdIn(searchString,currentEpicIds, pagination);
        }

        List<Epic> allEpicsList = allEpics.getContent();

        logger.info("allEpicsList -- " + allEpicsList.size());

        return new PaginationResponse(allEpicsList, allEpics.getTotalPages());
    }


    @PostMapping("/podmodel/epic/stories/{epicId}")
    public PaginationResponse getEpicStories(@PathVariable Long epicId,
                                             @Valid @RequestBody PaginationRequest paginationRequest) {
        logger.info("---!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!getEpicStories---");

        Pageable pagination = getPageableObject(paginationRequest);

        String searchString = paginationRequest.getSearchString();

        Page<Story> allStoriesPage = null;

        if (searchString == null || searchString.trim().equals("")) {
            allStoriesPage = storyRepository.findAllByEpicAndActive(epicId, pagination);
        } else {
            // allStoriesPage = storyRepository.findAllByInputStringAndActive(searchString,
            // pagination);
        }

        List<Story> allStoryList = allStoriesPage.getContent();

        logger.info("allStoryList -- " + allStoryList.size());

        List<StoryResponse> allStoryResponseList = new ArrayList<StoryResponse>();

        for (Story story : allStoryList) {
            allStoryResponseList.add(new StoryResponse(story));
        }

        return new PaginationResponse(allStoryResponseList, allStoriesPage.getTotalPages());
    }

    @GetMapping("/podmodel/epic/nopage/list")
    public List<Epic> getAllNoPageEpics() {
        logger.info("---getAllNoPageEpics---");
        List<Epic> allEpics = epicRepository.findByIsActiveOrderByIdDesc(true);

        return allEpics;
    }

    @GetMapping("/podmodel/getstoriesnumber/{epicid}")
    public Long getstoriesnumber(@PathVariable Long epicid) {

		return 0L;
	}

    @GetMapping("/podmodel/getepicbyid/{id}")
    private Optional<Epic> getepicbyid(@PathVariable(name = "id") long id) {
        return epicRepository.findById(id);
    }

    @PostMapping("/podmodel/sprint/add")
    public ResponseEntity<?> createSprint(@RequestBody SprintCreateRequest sprint) {
        try {
            sprintService.createSprint(sprint);
            return ResponseEntity.ok(new ApiResponse(true, "Sprint created successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/podmodel/sprint/editSprint/{oldId}")
    public Sprint getEditSprint(@PathVariable Long oldId) {
        return sprintService.getEditSprintDetails(oldId);
    }

    @PostMapping("/podmodel/sprint/saveEdit/{id}")
    public ResponseEntity<?> saveEditSprint(@RequestBody SprintCreateRequest sprint, @PathVariable Long id) {
        try {
            logger.info("Id" + id + "Sprint " + sprint + " Edited by " + getMyAssociateId());
            sprintService.saveEditSprint(sprint, id);
            return ResponseEntity.ok(new ApiResponse(true, "Edited"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/podmodel/sprint/addscrummaster")
    public ResponseEntity<?> setSprintScrumMaster(@RequestBody SprintAddScrumMasterOrTechLeadRequest request) {
        try {
            logger.info("request.getSprintId(), request.getAssociateId():" + request.getSprintId() + ":"
                    + request.getAssociateId());
            Long newId = sprintService.addSprintScrumMaster(request.getSprintId(), request.getAssociateId());
            return ResponseEntity.ok(new ApiResponse(true, newId + ""));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/podmodel/sprint/addtechnicallead")
    public ResponseEntity<?> setSprintTechnicalLead(@RequestBody SprintAddScrumMasterOrTechLeadRequest request) {
        try {
            Long newId = sprintService.addSprintTechnicalLead(request.getSprintId(), request.getAssociateId());
            return ResponseEntity.ok(new ApiResponse(true, newId + ""));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/podmodel/sprint/scrumorlead") // active and not closed and not cancelled
    public PaginationResponse getAllScrumMasterSprints(@Valid @RequestBody PaginationRequest paginationRequest) {
        logger.info("---getAllActiveSprints---");

        Pageable pagination = getPageableObject(paginationRequest);

        String searchString = paginationRequest.getSearchString();

        Page<Sprint> allSprintsPage = null;

        if (searchString == null || searchString.trim().equals("")) {
            allSprintsPage = sprintRepository.findByScrumMasterIdAndIsActiveAndSprintStatusInOrderByIdDesc(
                    getMyAssociateId(), true, sprintService.getActiveSprintStatuses(), pagination);
        } else {
            allSprintsPage = sprintRepository.findAllByInputStringAndActive(searchString, pagination);
        }

        List<Sprint> allSprintList = allSprintsPage.getContent();

        logger.info("allSprintList -- " + allSprintList.size());

        return new PaginationResponse(allSprintList, allSprintsPage.getTotalPages());
    }

    @PostMapping("/podmodel/sprint/activelist") // active and not closed and not cancelled
    public PaginationResponse getAllActiveSprints(@Valid @RequestBody PaginationRequest paginationRequest) {
        logger.info("---getAllActiveSprints---");

        Pageable pagination = getPageableObject(paginationRequest);

        String searchString = paginationRequest.getSearchString();

        Page<Sprint> allSprintsPage = null;

        if (searchString == null || searchString.trim().equals("")) {
            allSprintsPage = sprintRepository.findByIsActiveAndSprintStatusInOrderByIdDesc(true,
                    sprintService.getActiveSprintStatuses(), pagination);
        } else {
            allSprintsPage = sprintRepository.findAllByInputStringAndActive(searchString, pagination);
        }

        List<Sprint> allSprintList = allSprintsPage.getContent();

        logger.info("allSprintList -- " + allSprintList.size());

        return new PaginationResponse(allSprintList, allSprintsPage.getTotalPages());
    }

    @PostMapping("/podmodel/sprint/inactivelist") // active and not closed and not cancelled
    public PaginationResponse getAllInActiveSprints(@Valid @RequestBody PaginationRequest paginationRequest) {
        logger.info("---getAl    lIn  ActiveSprints---");

        Pageable pagination = getPageableObject(paginationRequest);

        String searchString = paginationRequest.getSearchString();

        Page<Sprint> allSprintsPage = null;

        if (searchString == null || searchString.trim().equals("")) {
            allSprintsPage = sprintRepository.findByIsActiveAndSprintStatusInOrderByIdDesc(true,
                    sprintService.getInActiveSprintStatuses(), pagination);
        } else {
            allSprintsPage = sprintRepository.findAllByInputStringAndActive(searchString, pagination);
        }

        List<Sprint> allSprintList = allSprintsPage.getContent();

        logger.info("allSprintList -- " + allSprintList.size());

        return new PaginationResponse(allSprintList, allSprintsPage.getTotalPages());
    }

    @GetMapping("/podmodel/nominatedscrummasters")
    public List getNominatedScrumMasters() throws Exception {
        List<HeadCount> list = headCountRepo.findAssociatesWithPODRole(PODConstants.POD_ROLE_SM,
                headCountService.getLatestReportId());
        return list;
    }

    @GetMapping("/podmodel/nominatedtechnicalleads")
    public List getNominatedTechnicalLeads() throws Exception {
        List<HeadCount> list = headCountRepo.findAssociatesWithPODRole(PODConstants.POD_ROLE_TL,
                headCountService.getLatestReportId());
        return list;
    }

    @GetMapping("/podmodel/nominateddevelopers")
    public List getNominatedDevelopers() throws Exception {
        List<HeadCount> list = headCountRepo.findAssociatesWithPODRole(PODConstants.POD_ROLE_DEV,
                headCountService.getLatestReportId());
        return list;
    }

    @GetMapping("/podmodel/sprint/cancel/{id}")
    public ResponseEntity<?> cancelSprint(@PathVariable Long id) {
        try {
            sprintService.cancelSprint(id);
            logger.info("Sprint " + id + " cancelled by " + getMyAssociateId());
            return ResponseEntity.ok(new ApiResponse(true, "Cancelled"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/podmodel/sprint/complete/{id}")
    public ResponseEntity<?> completeSprint(@PathVariable Long id) {
        try {
            sprintService.completeSprint(id);
            logger.info("Sprint " + id + " completed by " + getMyAssociateId());
            return ResponseEntity.ok(new ApiResponse(true, "Completed"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/podmodel/sprint/history/{originalId}")
    public List<Sprint> getSprintHistory(@PathVariable Long originalId) {
        return sprintService.getSprintHistory(originalId);
    }

    //
    @PutMapping("/podmodel/epic/edit/{epicId}")
    public ApiResponse editEpic(@PathVariable long epicId, @RequestBody EpicRequest epicRequest) {
        Epic epic = epicRepository.findById(epicId).orElse(null);
        epic.setName(epicRequest.getName());
        epic.setDescription(epicRequest.getDescription());
        epic.setEta(epicRequest.getEta());
        epic.setExpectedStoryPoint(epicRequest.getExpectedStoryPoint());

		Epic epicstory = epicService.editEpic(epic);
		return new ApiResponse(true, epicstory.getId().toString());
	}

	@GetMapping("/podmodel/dashboard/main")
	public ArrayList<PODDashboardListItem> getMyDashboard() {
		logger.info("getMyDashboard");
		Category category = null;
		ArrayList<PODDashboardListItem> list = new ArrayList<PODDashboardListItem>();
		List<AssociateAccess> associateAccessList = null;
		Set<String> parentCustomerNames = null;
		List<String> dbNames = null;
		List<HeadCount> headCountList = null;

        HashMap<String, PODDashboardListItem> mapPODDashboardListItem = new HashMap<String, PODDashboardListItem>();

        Long reportId = headCountService.getLatestReportId();

        String[] billableCategories = {BillableConstants.FULLY_BILLLABLE, BillableConstants.PARTIALLY_BILLABLE};

        String[] allValidStoryArr = {PODConstants.SPRINT_STORY_READY, PODConstants.SPRINT_STORY_INPROGRESS,
                PODConstants.SPRINT_STORY_COMPLETED, PODConstants.SPRINT_STORY_ACCEPTED};

        String[] allValidEpicStatusArr = {PODConstants.EPIC_STATUS_DRAFT, PODConstants.EPIC_STATUS_READY,
                PODConstants.EPIC_STATUS_INPROGRESS, PODConstants.EPIC_STATUS_COMPLETED};

        String[] allCompletedEpicStatusArr = {PODConstants.EPIC_STATUS_COMPLETED};

        String[] allValidSprintStatusArr = {PODConstants.SPRINT_STATUS_DRAFT, PODConstants.SPRINT_STATUS_READY,
                PODConstants.SPRINT_STATUS_INPROGRESS, PODConstants.SPRINT_STATUS_COMPLETED};

        String[] allCompletedSprintStatusArr = {PODConstants.SPRINT_STATUS_COMPLETED};

		String[] allCompletedValidStoryArr = { PODConstants.SPRINT_STORY_COMPLETED,
				PODConstants.SPRINT_STORY_ACCEPTED };

		List<Category> billeableCategoryList = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
				BillableConstants.CATEGORY_BILLABILITY_GROUP, BillableConstants.CATEGORY_BILLABILITY_KEY_BILL,
				Arrays.asList(billableCategories));

        List<Category> allValidStoryStatus = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
                PODConstants.STORY_STATUS, PODConstants.STORY_STATUS, Arrays.asList(allValidStoryArr));

        List<Category> allValidEpicStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
                PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, Arrays.asList(allValidEpicStatusArr));

        List<Category> allCompletedEpicStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
                PODConstants.EPIC_STATUS, PODConstants.EPIC_STATUS, Arrays.asList(allCompletedEpicStatusArr));

        List<Category> allValidSprintStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
                PODConstants.SPRINT_STATUS, PODConstants.SPRINT_STATUS, Arrays.asList(allValidSprintStatusArr));

        List<Category> allCompletedSprintStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
                PODConstants.SPRINT_STATUS, PODConstants.SPRINT_STATUS, Arrays.asList(allCompletedSprintStatusArr));

		List<Category> allValidCompletedStoryStatus = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
				PODConstants.STORY_STATUS, PODConstants.STORY_STATUS, Arrays.asList(allCompletedValidStoryArr));

		List<String> genCGrades = GradeMapping.getOriginalGenCGradeList();

        List<Object[]> valueList = null;

		switch (getMyMainRole()) {
		case BUHead:
			// case SBUHead:
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

			associateAccessList = associateAccessRepo.findByAccessTypeAndAssociateId(category, getMyAssociateId());

			valueList = headCountRepo.getTotalGenCByPDL(dbNames, category, genCGrades, reportId);

			for (Object[] headCount : valueList) {
				PODDashboardListItem item = mapPODDashboardListItem.get(headCount[0]);

				if (item == null) {
					item = new PODDashboardListItem();
					item.setPortfolio(headCount[0].toString());
					logger.info("PDL = " + headCount[0].toString());
					mapPODDashboardListItem.put(headCount[0].toString(), item);
				}
			}

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALGENCS, mapPODDashboardListItem);

			valueList = sprintRepository.getSprintsGenCByPDL(dbNames, category, genCGrades, true, allValidStoryStatus,
					allValidSprintStatuses, reportId);

			setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALSPRINTS, mapPODDashboardListItem);

                break;
            // case BUHead:
            case SBUHead:
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

			associateAccessList = associateAccessRepo.findByAccessTypeAndAssociateId(category, getMyAssociateId());

			valueList = headCountRepo.getTotalGenCByPDL(dbNames, category, genCGrades, reportId);

                for (Object[] headCount : valueList) {
                    PODDashboardListItem item = mapPODDashboardListItem.get(headCount[0]);

				if (item == null) {
					item = new PODDashboardListItem();
					item.setPortfolio(headCount[0].toString());
					logger.info("PDL = " + headCount[0].toString());
					mapPODDashboardListItem.put(headCount[0].toString(), item);
				}
			}

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALGENCS, mapPODDashboardListItem);

                valueList = billablePlanRepository.getBilleableGenCByPDL(dbNames, category, genCGrades,
                        billeableCategoryList, reportId);
                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_BILLABLEGENCS, mapPODDashboardListItem);

			// PODDASHBOARD_NOMINATEDGENCS
			valueList = podRoleRepo.getTotalNominatedDevelopersByPDL(dbNames, category, genCGrades,
					PODConstants.POD_ROLE_DEV, reportId);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_NOMINATEDGENCS, mapPODDashboardListItem);

                valueList = storyRepository.countAssociateWithStoriesByPDL(dbNames, category, genCGrades,
                        allValidStoryStatus, true, reportId);

			setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_GENCSWITHSTORIES, mapPODDashboardListItem);
			setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_GENCSWITHNOSTORIES, mapPODDashboardListItem);

			valueList = epicRepository.countEpicsByPDL(dbNames, category, genCGrades, true, allValidStoryStatus,
					allValidEpicStatuses, reportId);

			setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALEPICS, mapPODDashboardListItem);

                valueList = epicRepository.countEpicsByPDL(dbNames, category, genCGrades, true, allValidStoryStatus,
                        allValidEpicStatuses, reportId);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALEPICS, mapPODDashboardListItem);

                valueList = epicRepository.countEpicsByPDL(dbNames, category, genCGrades, true, allValidStoryStatus,
                        allCompletedEpicStatuses, reportId);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_COMPLETEDEPICS, mapPODDashboardListItem);

                valueList = sprintRepository.getSprintsGenCByPDL(dbNames, category, genCGrades, true, allValidStoryStatus,
                        allValidSprintStatuses, reportId);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALSPRINTS, mapPODDashboardListItem);

                valueList = sprintRepository.getSprintsGenCByPDL(dbNames, category, genCGrades, true, allValidStoryStatus,
                        allCompletedSprintStatuses, reportId);

			setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_COMPLETEDSPRINTS, mapPODDashboardListItem);
			
			valueList = storyRepository.countTotalStoriesByPDL(dbNames, category, genCGrades,
					allValidStoryStatus, true, reportId);

			setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALSTORIES, mapPODDashboardListItem);
			
			valueList = storyRepository.countTotalStoriesByPDL(dbNames, category, genCGrades,
					allValidCompletedStoryStatus, true, reportId);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_COMPLETEDSTORIES, mapPODDashboardListItem);

                logger.info("----------------headCountList1.size()------>" + valueList.size());

                break;
            case PDL:
            case EDL:
                logger.info("getMyDashboard:EDL/PDL");
                parentCustomerNames = new HashSet<String>();
                // Set<Long> projectIds = new HashSet<Long>();
                // Set<Long> allGenCIds = new HashSet<Long>();

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
                    // projectIds.add(headCount.getProjectId());
                    // if (GradeMapping.isGenC(headCount.getGrade()))
                    // allGenCIds.add(headCount.getAssociateId());
                }

                // PODDASHBOARD_BILLABLEGENCS
                valueList = billablePlanRepository.countBillableAssociatesByParentCustomer(
                        new ArrayList<String>(parentCustomerNames), reportId, billeableCategoryList, true, genCGrades);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_BILLABLEGENCS, mapPODDashboardListItem);

                // PODDASHBOARD_TOTALGENCS
                valueList = billablePlanRepository.countTotalAssociatesByParentCustomer(
                        new ArrayList<String>(parentCustomerNames), reportId, genCGrades);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALGENCS, mapPODDashboardListItem);

                // PODDASHBOARD_NOMINATEDGENCS
                valueList = podRoleRepo.countTotalDevelopersByParentCustomer(new ArrayList<String>(parentCustomerNames),
                        reportId, genCGrades, PODConstants.POD_ROLE_DEV);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_NOMINATEDGENCS, mapPODDashboardListItem);

                // PODDASHBOARD_TOTALSTORIES
                valueList = storyRepository.countTotalStoriesByParentCustomer(new ArrayList<String>(parentCustomerNames),
                        reportId, genCGrades, true, allValidStoryStatus);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALSTORIES, mapPODDashboardListItem);

                // PODDASHBOARD_GENCSWITHSTORIES && PODDASHBOARD_GENCSWITHNOSTORIES
                valueList = storyRepository.countAssociateWithStoriesByParentCustomer(
                        new ArrayList<String>(parentCustomerNames), reportId, genCGrades, true, allValidStoryStatus);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_GENCSWITHSTORIES, mapPODDashboardListItem);
                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_GENCSWITHNOSTORIES, mapPODDashboardListItem);

                // PODDASHBOARD_COMPLETEDSTORIES
                valueList = storyRepository.countTotalStoriesByParentCustomer(new ArrayList<String>(parentCustomerNames),
                        reportId, genCGrades, true, allValidCompletedStoryStatus);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_COMPLETEDSTORIES, mapPODDashboardListItem);

                valueList = epicRepository.countEpicsByParentCustomer(new ArrayList<String>(parentCustomerNames), reportId,
                        genCGrades, true, allValidStoryStatus, allValidEpicStatuses);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALEPICS, mapPODDashboardListItem);

                valueList = epicRepository.countEpicsByParentCustomer(new ArrayList<String>(parentCustomerNames), reportId,
                        genCGrades, true, allValidCompletedStoryStatus, allCompletedEpicStatuses);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_COMPLETEDEPICS, mapPODDashboardListItem);

                valueList = sprintRepository.countSprintsByParentCustomer(new ArrayList<String>(parentCustomerNames),
                        reportId, genCGrades, true, allValidStoryStatus, allValidSprintStatuses);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_TOTALSPRINTS, mapPODDashboardListItem);

                valueList = sprintRepository.countSprintsByParentCustomer(new ArrayList<String>(parentCustomerNames),
                        reportId, genCGrades, true, allValidStoryStatus, allCompletedSprintStatuses);

                setPODDashboardListItem(valueList, PODConstants.PODDASHBOARD_COMPLETEDSPRINTS, mapPODDashboardListItem);

                for (PODDashboardListItem item : new ArrayList<PODDashboardListItem>(mapPODDashboardListItem.values())) {
                    logger.info(item.toString());
                }

                break;

            default:
                break;
        }
        return new ArrayList<PODDashboardListItem>(mapPODDashboardListItem.values());
    }

    //	----------------------------------------------------------------------------------
    @PostMapping("/podmodel/MyAllEpics/list/{requestId}")
    public PaginationResponse getMyAllEpicsData(@PathVariable Long requestId, @Valid @RequestBody PaginationRequest paginationRequest) {
        logger.info("Id:-" + requestId);
        Category category = null;
        ArrayList<PODDashboardListItem> list = new ArrayList<PODDashboardListItem>();
        List<AssociateAccess> associateAccessList = null;
        Set<String> parentCustomerNames = null;
        List<String> dbNames = null;
        List<HeadCount> headCountList = null;
        Page<Epic> myAllEpic = null;
        List<Epic> allEpicsList = null;
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
                List<Long> epicIds = epicRepository.getMyEpicsByParentCustomer(new ArrayList<String>(parentCustomerNames), reportId,
                        genCGrades, true, allValidStoryStatus, allValidEpicStatuses);
                logger.info("getMyAllEpicIds:EDL/PDL" + epicIds);

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
                    }
                }

                allEpicsList = myAllEpic.getContent();

                logger.info("allEpicsList -- " + allEpicsList.size());

                break;

            default:
                break;
        }
        return new PaginationResponse(allEpicsList, myAllEpic.getTotalPages());
    }

    @PostMapping("/podmodel/MyAllSprints/list/{requestId}")
    public PaginationResponse getMyAllSprintData(@PathVariable Long requestId, @Valid @RequestBody PaginationRequest paginationRequest) {
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

                List<Long> sprintsId = sprintRepository.myAllSprintsIdsByParentCustomer(new ArrayList<String>(parentCustomerNames),
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

    @PostMapping("/podmodel/MyAllStory/list/{requestId}")
    public PaginationResponse getMyAllStoryData(@PathVariable Long requestId, @Valid @RequestBody PaginationRequest paginationRequest) {
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
                List<Long> epicIds = epicRepository.getMyEpicsByParentCustomer(new ArrayList<String>(parentCustomerNames), reportId,
                        genCGrades, true, allValidStoryStatus, allValidEpicStatuses);
                List<Long> sprintsId = sprintRepository.myAllSprintsIdsByParentCustomer(new ArrayList<String>(parentCustomerNames),
                        reportId, genCGrades, true, allValidStoryStatus, allValidSprintStatuses);
                List<Long> storyIds = storyRepository.getMyTotalStoriesByParentCustomer(new ArrayList<String>(parentCustomerNames),
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

    //--------------------------------------------------------------------------------------
    public void setPODDashboardListItem(List<Object[]> valueList, int valueIdx,
                                        HashMap<String, PODDashboardListItem> mapPODDashboardListItem) {
        for (Object[] objectArr : valueList) {
            PODDashboardListItem item = mapPODDashboardListItem.get(objectArr[0].toString());
            Long count = PMUtils.getLongValue(objectArr[1].toString());
            if (item != null) {
                switch (valueIdx) {
                    case PODConstants.PODDASHBOARD_TOTALGENCS:// done
                        item.setTotalGenCs(count);
                        break;
                    case PODConstants.PODDASHBOARD_BILLABLEGENCS:// done
                        item.setBillableGenCs(count);
                        break;
                    case PODConstants.PODDASHBOARD_NOMINATEDGENCS:// done
                        item.setNominatedGenCs(count);
                        break;
                    case PODConstants.PODDASHBOARD_GENCSWITHSTORIES:// done
                        item.setGenCsWithStories(count);
                        break;
                    case PODConstants.PODDASHBOARD_GENCSWITHNOSTORIES:// done
                        item.setGenCsWithNoStories(item.getNominatedGenCs() - item.getGenCsWithStories());
                        break;
                    case PODConstants.PODDASHBOARD_TOTALEPICS:// done
                        item.setTotalEpics(count);
                        break;
                    case PODConstants.PODDASHBOARD_COMPLETEDEPICS: // done
                        item.setCompletedEpics(count);
                        break;
                    case PODConstants.PODDASHBOARD_TOTALSPRINTS:
                        item.setTotalSprints(count);
                        break;
                    case PODConstants.PODDASHBOARD_COMPLETEDSPRINTS:
                        item.setCompletedSprints(count);
                        break;
                    case PODConstants.PODDASHBOARD_TOTALSTORIES:// done
                        item.setTotalStories(count);
                        break;
                    case PODConstants.PODDASHBOARD_COMPLETEDSTORIES:// done
                        item.setCompletedStories(count);
                        break;

                }

            }
        }

    }

	public Set<String> getParentCustomerByRole() {

		Category category = null;
		List<AssociateAccess> associateAccessList = null;
		Set<String> parentCustomerNames = new HashSet<String>();
		switch (getMyMainRole()) {
		case BUHead:
			break;
		case SBUHead:
			break;
		case PDL:
		case EDL:
			logger.info("getMyDashboard:EDL/PDL");
			// Set<Long> projectIds = new HashSet<Long>();
			// Set<Long> allGenCIds = new HashSet<Long>();

			category = categoryRepository.findByCatGroupAndGroupKeyAndGroupValue(
					AccessGroupConstants.ACCESSGROUP_CATEGORY_GROUP, AccessGroupConstants.ACCESSGROUP_CATEGORY_KEY,
					getMyMainRole().name());

			associateAccessList = associateAccessRepo.findByAccessTypeAndAssociateId(category, getMyAssociateId());

			ArrayList<String> dbNames = new ArrayList<String>();
			for (AssociateAccess associateAccess : associateAccessList) {
				dbNames.add(associateAccess.getObjectName());
			}

			Long reportId = headCountService.getLatestReportId();

			List<HeadCount> headCountList = headCountRepo.findByParentCustomerNameInAndReportId(dbNames, reportId);

			for (HeadCount headCount : headCountList) {

				parentCustomerNames.add(headCount.getParentCustomerName());
				// projectIds.add(headCount.getProjectId());
				// if (GradeMapping.isGenC(headCount.getGrade()))
				// allGenCIds.add(headCount.getAssociateId());
			}
			break;

		default:
			break;
		}
		return parentCustomerNames;
	}

	// ======================================================

	@PostMapping("/podrole/getMyAllGencsWithStories")
	public PaginationResponse getMyAllGencsWithStories(@Valid @RequestBody PaginationRequest paginationRequest)
			throws Exception {
		Page<GencsWithStoriesView> myAllAssociatesWithStories = null;
		List<GencsWithStoriesView> allGensWithStoriesData = null;
		Pageable pagination = null;
		Sort sortTechnique = null;
		if (paginationRequest.getSortColumn() != null
				&& !paginationRequest.getSortColumn().trim().equalsIgnoreCase("")) {
			String sortColumn = paginationRequest.getSortColumn();

			if (paginationRequest.getIsAscending())
				if (sortColumn.equalsIgnoreCase("associateId")) {
					sortTechnique = Sort.by("associateId").ascending();
				} else
					sortTechnique = Sort.by(sortColumn).ascending();
			else {
				if (sortColumn.equalsIgnoreCase("associateId")) {
					sortTechnique = Sort.by("associateId").descending();
				} else
					sortTechnique = Sort.by(sortColumn).descending();
			}

			pagination = PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize(), sortTechnique);
		} else {
			sortTechnique = Sort.by("associateId").descending();
			pagination = PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize(), sortTechnique);
		}
		String searchString = paginationRequest.getSearchString();

		switch (getMyMainRole()) {
		case BUHead:
			break;
		case SBUHead:
			break;
		case PDL:
		case EDL:

			String[] allValidStoryArr = { PODConstants.SPRINT_STORY_READY, PODConstants.SPRINT_STORY_INPROGRESS,
					PODConstants.SPRINT_STORY_COMPLETED };
			List<Category> storyStatuses = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
					PODConstants.STORY_STATUS, PODConstants.STORY_STATUS, Arrays.asList(allValidStoryArr));
			List<String> genCGrades = GradeMapping.getOriginalGenCGradeList();
			if (searchString == null || searchString.trim().equals("")) {
				myAllAssociatesWithStories = storyRepository.findAssociateWithStoriesForEDLOrPDL(
						new ArrayList<String>(getParentCustomerByRole()), headCountService.getLatestReportId(),
						genCGrades, true, storyStatuses, pagination);
			} else {
				// myAllNominatedAssociates=podRoleRepo.findAllByInputStringAndElectorAssociateId(searchString,getMyAssociateId(),pagination);
			}
			allGensWithStoriesData = myAllAssociatesWithStories.getContent();
			break;
		default:
			break;
		}

		return new PaginationResponse(allGensWithStoriesData, myAllAssociatesWithStories.getTotalPages());
	}

	@PostMapping("/podrole/getMyAllBillableGencsWithGrade")
	public PaginationResponse getMyAllGencsWithNoStories(@Valid @RequestBody PaginationRequest paginationRequest)
			throws Exception {
		Page<BillableGencsWithGradeView> myAllBillableAssociates = null;
		List<BillableGencsWithGradeView> myAllBillableAssociatesData = null;
		Pageable pagination = null;
		Sort sortTechnique = null;
		if (paginationRequest.getSortColumn() != null
				&& !paginationRequest.getSortColumn().trim().equalsIgnoreCase("")) {
			String sortColumn = paginationRequest.getSortColumn();

			if (paginationRequest.getIsAscending())
				if (sortColumn.equalsIgnoreCase("associateId")) {
					sortTechnique = Sort.by("associateId").ascending();
				} else
					sortTechnique = Sort.by(sortColumn).ascending();
			else {
				if (sortColumn.equalsIgnoreCase("associateId")) {
					sortTechnique = Sort.by("associateId").descending();
				} else
					sortTechnique = Sort.by(sortColumn).descending();
			}

			pagination = PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize(), sortTechnique);
		} else {
			sortTechnique = Sort.by("associateId").descending();
			pagination = PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize(), sortTechnique);
		}
		String searchString = paginationRequest.getSearchString();

		switch (getMyMainRole()) {
		case BUHead:
			break;
		case SBUHead:
			break;
		case PDL:
		case EDL:

			String[] billableCategories = { BillableConstants.FULLY_BILLLABLE, BillableConstants.PARTIALLY_BILLABLE };

			List<Category> billeableCategoryList = categoryRepository.findByCatGroupAndGroupKeyAndGroupValueIn(
					BillableConstants.CATEGORY_BILLABILITY_GROUP, BillableConstants.CATEGORY_BILLABILITY_KEY_BILL,
					Arrays.asList(billableCategories));
			List<String> genCGrades = GradeMapping.getOriginalGenCGradeList();

			if (searchString == null || searchString.trim().equals("")) {
				myAllBillableAssociates = billablePlanRepository.findBillableAssociatesByParentCustomer(
						new ArrayList<String>(getParentCustomerByRole()), headCountService.getLatestReportId(),
						billeableCategoryList, true, genCGrades, pagination);
			} else {
//		            myAllNominatedAssociates=podRoleRepo.findAllByInputStringAndElectorAssociateId(searchString,getMyAssociateId(),pagination);
			}
			myAllBillableAssociatesData = myAllBillableAssociates.getContent();
			break;
		default:
			break;
		}

		return new PaginationResponse(myAllBillableAssociatesData, myAllBillableAssociates.getTotalPages());
	}

    public static String changeOneInController(){
        System.out.println(change1);
        return change1;
    }

}