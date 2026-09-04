package com.reiwaxr.cq.cqham.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * Skill定義エンティティ
 */
@Getter
@Setter
public class AiSkillDefinition {
    private Integer id;
    private String skillCode;
    private String skillName;
    private String description;
    private String categoryName;
    private String tags;
    private String systemPrompt;
    private String inputTemplate;
    private String outputTemplate;
    private String exampleInput;
    private String exampleOutput;
    private Integer sortOrder;
    private boolean enabled;
    private boolean builtIn;
    private boolean deleted;

}