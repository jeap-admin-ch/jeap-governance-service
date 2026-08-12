package ch.admin.bit.jeap.governance.messagecontract.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mc_version_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageContractVersionStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mc_version_status_seq")
    @SequenceGenerator(name = "mc_version_status_seq", sequenceName = "mc_version_status_id_seq")
    private Long id;
    private String appName;
    private String appVersion;
    private String messageType;
    private String usedVersion;
    private String latestVersion;
    private String topic;
    private String role;
    private boolean upToDate;

    public MessageContractVersionStatus(String appName, String appVersion, String messageType, String usedVersion,
                                        String latestVersion, String topic, String role, boolean upToDate) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.messageType = messageType;
        this.usedVersion = usedVersion;
        this.latestVersion = latestVersion;
        this.topic = topic;
        this.role = role;
        this.upToDate = upToDate;
    }
}
