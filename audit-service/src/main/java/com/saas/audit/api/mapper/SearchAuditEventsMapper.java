package com.saas.audit.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.audit.api.dto.SearchAuditEventsRequest;
import com.saas.audit.application.query.SearchAuditEventsQuery;

@Component
public class SearchAuditEventsMapper implements Mapper<SearchAuditEventsRequest, SearchAuditEventsQuery> {

    @Override
    public SearchAuditEventsQuery map(SearchAuditEventsRequest request) {
        return new SearchAuditEventsQuery(
                request.type(),
                request.projectId(),
                request.onlyDenied(),
                request.from(),
                request.to());
    }
}
