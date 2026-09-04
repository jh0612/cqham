package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Agent固定模板定义
 */
@Setter
@Getter
public class AiAgentTemplate {
    private Integer id;
    private String templateCode;
    private String templateName;
    private String description;
    private Integer sortOrder;
    private boolean enabled;
    private boolean builtIn;

}
