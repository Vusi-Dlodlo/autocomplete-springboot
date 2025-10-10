// NEW: Safe overload (original method remains unchanged; this prevents 500s in POST)
private void updateApiDetailsTable(Long requestId, String description, String personalInfo, 
                                   String specialPersonalInfo, String cardInfo, String classification, 
                                   String status, WorkItem workItem) throws Exception {
    ApiDetails apiList = apiDetailsRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(API_INFO_UPDATE));
    
    apiList.setDescription(description);
    apiList.setPersonalInfo(personalInfo);
    apiList.setSpecialPersonalInfo(specialPersonalInfo);
    apiList.setCardInfo(cardInfo);
    apiList.setApiClassification(classification);
    apiList.setWorkItemId(workItem);  // Null-safe
    
    if (StringUtils.isNotBlank(status) && "verified".equalsIgnoreCase(status)) {
        apiList.setDescriptionStatus(status);
    }
    
    apiDetailsRepository.save(apiList);
    log.debug("Updated ApiDetails for ID: {}", requestId);
}
