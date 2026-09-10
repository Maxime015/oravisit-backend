package com.orabank.backend.dto;

import com.orabank.backend.entity.Direction;

/** Nombre de visites pour une direction, sur la periode du dashboard. */
public record DirectionCountResponse(Direction direction, long total) {
}
