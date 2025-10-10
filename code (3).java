@RequestMapping(value = {"/api/update"}, method = RequestMethod.POST)
public String updateApiDetails(@ModelAttribute("service") ApiPageItem form,  // Binds form to existing ApiPageItem
                               Model model, PSSAuthentication pssAuthentication, 
                               HttpSession session, RedirectAttributes redirectAttributes,
                               @RequestParam(value = "api", required = true) String api,  // From hidden form field
                               @RequestParam(value = "version", required = true) String version,
                               @RequestParam(value = "systemName", required = false) String systemName,
                               @RequestParam(value = "workitemid", required = false) String workItemId) {  // From form if editable

    try {
        // Basic auth/setup (same as in GET /api)
        authCommonFunctions.setUserSystemAccess(pssAuthentication.getAuthentication(), model, session, 
                pssAuthentication.getUserAccess(), pssAuthentication.getSystemNames(), 
                systemName, pssAuthentication.getName(), pssAuthentication.getUserName());

        // Fetch current ApiDetails to check status and get ID (your existing method)
        ApiDetails apiDetail = fetchApiDetails(api);
        if (apiDetail == null) {
            log.warn("ApiDetails not found for endpoint: {}", api);
            redirectAttributes.addFlashAttribute("error", "API not found. Cannot update.");
            return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
        }

        // Authorization check (from your existing logic)
        String providerName = api.split("/")[1];
        if (!isUserAuthorisedForEnvironmentsAndSystem((String) session.getAttribute("requesterName"), DEV_ENVIRONMENT, providerName)) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to update this API.");
            return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
        }

        // Manual validation (adapted from your specified inline logic; prevents binding errors)
        String apiDescription = form.getDescription();  // From ApiPageItem
        String personalInfo = form.getPersonalInfo();
        String specialPersonalInfo = form.getSpecialPersonalInfo();
        String cardInfo = form.getCardInfo();
        String classification = form.getApiClassification();  // Assume getter
        String status = form.getDescriptionStatus();

        if (apiDetail.getDescriptionStatus() == null && StringUtils.isBlank(status)) {
            redirectAttributes.addFlashAttribute("error", VERIFY_STATUS_EMPTY_ERROR_MESSAGE);
            return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
        } else if (StringUtils.isBlank(apiDescription)) {
            redirectAttributes.addFlashAttribute("error", API_DESC_EMPTY_ERROR_MESSAGE);
            return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
        } else if ("undefined".equalsIgnoreCase(personalInfo)) {
            redirectAttributes.addFlashAttribute("error", API_PI_EMPTY_ERROR_MESSAGE);
            return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
        } else if ("".equalsIgnoreCase(specialPersonalInfo)) {  // Your specified check
            redirectAttributes.addFlashAttribute("error", API_SPI_EMPTY_ERROR_MESSAGE);
            return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
        } else if ("undefined".equalsIgnoreCase(cardInfo)) {  // Your specified check
            redirectAttributes.addFlashAttribute("error", API_PCI_EMPTY_ERROR_MESSAGE);
            return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
        } else if (API_CLASSIFICATION_NOT_SELECTED.equalsIgnoreCase(classification)) {  // Your specified check
            redirectAttributes.addFlashAttribute("error", API_CLASSIFICATION_EMPTY_ERROR_MESSAGE);
            return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
        } else {
            // Safe WorkItem resolution (your specified logic, adapted for form)
            WorkItem workItem = null;
            String formWorkItemId = StringUtils.isNotBlank(workItemId) ? workItemId : 
                    (form.getWorkItemId() != null ? 
                     (form.getWorkItemId() instanceof WorkItem ? ((WorkItem) form.getWorkItemId()).getWiIdentifier() : form.getWorkItemId().toString()) : null);
            if (StringUtils.isNotBlank(formWorkItemId)) {
                Optional<WorkItem> wiOpt = workItemRepository.findByWiIdentifier(formWorkItemId);
                if (wiOpt.isPresent()) {
                    workItem = wiOpt.get();
                } else {
                    log.warn("WorkItem not found for ID: {} during update", formWorkItemId);
                    redirectAttributes.addFlashAttribute("error", "Invalid Work Item ID.");
                    return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
                }
            }
            // Call the new safe overload
            updateApiDetailsTable(apiDetail.getId(), apiDescription, personalInfo, specialPersonalInfo, cardInfo, classification, status, workItem);
        }

        // Success
        redirectAttributes.addFlashAttribute("success", "API details updated successfully.");
        log.info("API {} updated by user {}", api, pssAuthentication.getName());

    } catch (ResourceNotFoundException e) {
        log.error("Resource not found during API update for {}: {}", api, e.getMessage(), e);
        redirectAttributes.addFlashAttribute("error", API_INFO_UPDATE);  // Your constant
    } catch (Exception e) {
        log.error("Error updating API {}: {}", api, e.getMessage(), e);
        redirectAttributes.addFlashAttribute("error", API_UPDATE_ERROR_MESSAGE);
    }

    // Redirect back to GET /api (Post-Redirect-Get)
    return "redirect:/api?api=" + api + "&version=" + version + "&systemName=" + systemName;
}
