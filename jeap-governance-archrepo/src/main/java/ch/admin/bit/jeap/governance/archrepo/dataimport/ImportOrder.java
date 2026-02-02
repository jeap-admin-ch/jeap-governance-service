package ch.admin.bit.jeap.governance.archrepo.dataimport;

import lombok.experimental.UtilityClass;

@UtilityClass
class ImportOrder {

    static final int SYSTEM_IMPORT_ORDER = org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
    static final int REST_API_RELATION_IMPORT_ORDER = 10;
    static final int API_DOC_VERSION_IMPORT_ORDER = 11;
    static final int DATA_BASE_SCHEMA_VERSION_ORDER = 13;
    static final int REACTION_GRAPHS_LAST_MODIFIED_AT_ORDER = 14;

}
