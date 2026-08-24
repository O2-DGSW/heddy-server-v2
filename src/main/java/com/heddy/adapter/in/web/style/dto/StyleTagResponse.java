package com.heddy.adapter.in.web.style.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.heddy.domain.style.model.StyleTag;
import com.heddy.domain.style.model.StyleTagCategory;

import java.util.UUID;

public record StyleTagResponse(
        @JsonProperty("style_tag_id") UUID styleTagId,
        @JsonProperty("tag_name") String tagName,
        StyleTagCategory category
) {
    public static StyleTagResponse from(StyleTag styleTag) {
        return new StyleTagResponse(
                styleTag.styleTagId(), styleTag.tagName(), styleTag.category());
    }
}
