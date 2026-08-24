package com.heddy.adapter.in.web.style.dto;

import com.heddy.domain.style.model.StyleTag;

import java.util.List;

public record StyleTagsResponse(List<StyleTagResponse> items) {
    public static StyleTagsResponse from(List<StyleTag> styleTags) {
        return new StyleTagsResponse(
                styleTags.stream().map(StyleTagResponse::from).toList());
    }
}
