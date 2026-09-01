package com.heddy.adapter.out.persistence.hairstyle;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class HairstyleTagQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    List<TagRow> findByHairstyleIds(Collection<UUID> hairstyleIds) {
        if (hairstyleIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT link.hairstyle_id, tag.style_tag_id, tag.tag_name
                FROM hairstyle_style_tags link
                JOIN style_tags tag ON tag.style_tag_id = link.style_tag_id
                WHERE link.hairstyle_id IN (:ids)
                ORDER BY link.hairstyle_id, tag.style_tag_id
                """, java.util.Map.of("ids", hairstyleIds), (resultSet, rowNumber) -> new TagRow(
                resultSet.getObject("hairstyle_id", UUID.class),
                resultSet.getObject("style_tag_id", UUID.class),
                resultSet.getString("tag_name")));
    }

    record TagRow(UUID hairstyleId, UUID styleTagId, String tagName) { }
}
