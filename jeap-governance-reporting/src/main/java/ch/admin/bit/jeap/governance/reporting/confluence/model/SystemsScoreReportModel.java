package ch.admin.bit.jeap.governance.reporting.confluence.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class SystemsScoreReportModel {
    private String title;
    private List<SystemScoreReportModel> systemScores;
}
